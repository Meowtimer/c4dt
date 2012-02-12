package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.EntityRegion;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.SpecialScriptRules;
import net.arctics.clonk.parser.c4script.SpecialScriptRules.SpecialFuncRule;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.resources.IFile;

public final class StringLiteral extends Literal<String> {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public StringLiteral(String literal) {
		super(literal != null ? literal : ""); //$NON-NLS-1$
	}

	public String stringValue() {
		return getLiteral();
	}
	@Override
	public void doPrint(ExprWriter output, int depth) {
		output.append("\""); //$NON-NLS-1$
		output.append(stringValue());
		output.append("\""); //$NON-NLS-1$
	}

	@Override
	protected IType obtainType(DeclarationObtainmentContext context) {
		return PrimitiveType.STRING;
	}

	@Override
	public EntityRegion declarationAt(int offset, C4ScriptParser parser) {

		// first check if a string tbl entry is referenced
		EntityRegion result = StringTbl.entryForLanguagePref(stringValue(), start(), (offset-1), parser.containingScript(), true);
		if (result != null)
			return result;

		// look whether some special linking rule can be applied to this literal
		if (parent() instanceof CallFunc) {
			CallFunc parentFunc = (CallFunc) parent();
			int myIndex = parentFunc.indexOfParm(this);

			// delegate finding a link to special function rules
			SpecialFuncRule funcRule = parentFunc.specialRuleFromContext(parser, SpecialScriptRules.DECLARATION_LOCATOR);
			if (funcRule != null) {
				EntityRegion region = funcRule.locateEntityInParameter(parentFunc, parser, myIndex, offset, this);
				if (region != null) {
					return region;
				}
			}

		}
		return super.declarationAt(offset, parser);
	}
	
	@Override
	public String evaluateAtParseTime(IEvaluationContext context) {
		StringTbl.EvaluationResult r = StringTbl.evaluateEntries(context.script(), StringUtil.evaluateEscapes(getLiteral()), false);
		// getting over-the-top: trace back to entry in StringTbl file to which the literal needs to be completely evaluated to 
		if (r.singleDeclarationRegionUsed != null && getLiteral().matches("\\$.*?\\$"))
			context.reportOriginForExpression(this, r.singleDeclarationRegionUsed.region(), (IFile) r.singleDeclarationRegionUsed.concreteDeclaration().resource());
		else if (!r.anySubstitutionsApplied)
			context.reportOriginForExpression(this, new SourceLocation(context.codeFragmentOffset(), this), context.script().scriptFile());
		return r.evaluated;
	}

	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		
		// warn about overly long strings
		long max = parser.containingScript().index().engine().currentSettings().maxStringLen;
		if (max != 0 && getLiteral().length() > max) {
			parser.warningWithCode(ParserErrorCode.StringTooLong, this, getLiteral().length(), max);
		}
		
		// stringtbl entries
		// don't warn in #appendto scripts because those will inherit their string tables from the scripts they are appended to
		// and checking for the existence of the table entries there is overkill
		if (parser.hasAppendTo() || parser.containingScript().resource() == null)
			return;
		String value = getLiteral();
		int valueLen = value.length();
		// warn when using non-declared string tbl entries
		for (int i = 0; i < valueLen;) {
			if (i+1 < valueLen && value.charAt(i) == '$') {
				EntityRegion region = StringTbl.entryRegionInString(stringValue(), start(), (i+1));
				if (region != null) {
					StringTbl.reportMissingStringTblEntries(parser, region);
					i += region.region().getLength();
					continue;
				}
			}
			++i;
		}
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