package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.IType;
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
	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {

		// first check if a string tbl entry is referenced
		DeclarationRegion result = StringTbl.entryForLanguagePref(stringValue(), getExprStart(), (offset-1), parser.container(), true);
		if (result != null)
			return result;

		// look whether some special linking rule can be applied to this literal
		if (getParent() instanceof CallFunc) {
			CallFunc parentFunc = (CallFunc) getParent();
			int myIndex = parentFunc.indexOfParm(this);

			//  link to functions that are called indirectly
			
			SpecialFuncRule funcRule = parentFunc.specialRuleFromContext(parser, SpecialScriptRules.DECLARATION_LOCATOR);
			if (funcRule != null) {
				DeclarationRegion region = funcRule.locateDeclarationInParameter(parentFunc, parser, myIndex, offset, this);
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
			context.reportOriginForExpression(this, r.singleDeclarationRegionUsed.getRegion(), (IFile) r.singleDeclarationRegionUsed.getConcreteDeclaration().resource());
		else if (!r.anySubstitutionsApplied)
			context.reportOriginForExpression(this, new SourceLocation(context.codeFragmentOffset(), this), context.script().getScriptFile());
		return r.evaluated;
	}

	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		
		// warn about overly long strings
		long max = parser.container().getIndex().engine().currentSettings().maxStringLen;
		if (max != 0 && getLiteral().length() > max) {
			parser.warningWithCode(ParserErrorCode.StringTooLong, this, getLiteral().length(), max);
		}
		
		// stringtbl entries
		// don't warn in #appendto scripts because those will inherit their string tables from the scripts they are appended to
		// and checking for the existence of the table entries there is overkill
		if (parser.hasAppendTo() || parser.container().resource() == null)
			return;
		String value = getLiteral();
		int valueLen = value.length();
		// warn when using non-declared string tbl entries
		for (int i = 0; i < valueLen;) {
			if (i+1 < valueLen && value.charAt(i) == '$') {
				DeclarationRegion region = StringTbl.entryRegionInString(stringValue(), getExprStart(), (i+1));
				if (region != null) {
					StringTbl.reportMissingStringTblEntries(parser, region);
					i += region.getRegion().getLength();
					continue;
				}
			}
			++i;
		}
	}

	@Override
	public int identifierStart() {
		return getExprStart()+1;
	}

	@Override
	public int identifierLength() {
		return stringValue().length();
	}

}