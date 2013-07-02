package net.arctics.clonk.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.IEvaluationContext;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SpecialEngineRules;
import net.arctics.clonk.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.stringtbl.StringTbl;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.resources.IFile;

public final class StringLiteral extends Literal<String> {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final String literal;

	public StringLiteral(String literal) {
		this.literal =literal != null ? literal : ""; //$NON-NLS-1$
	}
	@Override
	public String literal() {
		return literal;
	}
	public String stringValue() {
		return literal;
	}
	@Override
	public void doPrint(ASTNodePrinter output, int depth) {
		output.append("\""); //$NON-NLS-1$
		output.append(stringValue());
		output.append("\""); //$NON-NLS-1$
	}

	@Override
	public EntityRegion entityAt(int offset, ExpressionLocator<?> locator) {

		// first check if a string tbl entry is referenced
		final EntityRegion result = StringTbl.entryForLanguagePref(stringValue(), start(), (offset-1), parent(Script.class), true);
		if (result != null)
			return result;

		// look whether some special linking rule can be applied to this literal
		if (parent() instanceof CallDeclaration) {
			final CallDeclaration parentFunc = (CallDeclaration) parent();
			final int myIndex = parentFunc.indexOfParm(this);
			// delegate finding a link to special function rules
			final SpecialFuncRule funcRule = parent(Declaration.class).engine().specialRules().funcRuleFor(parentFunc.name(), SpecialEngineRules.DECLARATION_LOCATOR);
			if (funcRule != null) {
				final EntityRegion region = funcRule.locateEntityInParameter(parentFunc, parent(Script.class), myIndex, offset, this);
				if (region != null)
					return region;
			}
		}
		return super.entityAt(offset, locator);
	}

	@Override
	public String evaluateStatic(IEvaluationContext context) {
		final String escapesEvaluated = StringUtil.evaluateEscapes(literal());
		if (context == null || context.script() == null)
			return escapesEvaluated;
		final StringTbl.EvaluationResult r = StringTbl.evaluateEntries(context.script(), escapesEvaluated, false);
		// getting over-the-top: trace back to entry in StringTbl file to which the literal needs to be completely evaluated to
		if (r.singleDeclarationRegionUsed != null && literal().matches("\\$.*?\\$"))
			context.reportOriginForExpression(this, r.singleDeclarationRegionUsed.region(), (IFile) r.singleDeclarationRegionUsed.entityAs(Declaration.class).resource());
		else if (!r.anySubstitutionsApplied)
			context.reportOriginForExpression(this, new SourceLocation(context.codeFragmentOffset(), this), context.script().file());
		return r.evaluated;
	}

	@Override
	public int identifierStart() {
		return start()+1;
	}

	@Override
	public int identifierLength() {
		return stringValue().length();
	}

}