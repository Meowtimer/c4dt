package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;
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

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private String literal;
	
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
		if (parent() instanceof CallDeclaration) {
			CallDeclaration parentFunc = (CallDeclaration) parent();
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
		StringTbl.EvaluationResult r = StringTbl.evaluateEntries(context.script(), StringUtil.evaluateEscapes(literal()), false);
		// getting over-the-top: trace back to entry in StringTbl file to which the literal needs to be completely evaluated to 
		if (r.singleDeclarationRegionUsed != null && literal().matches("\\$.*?\\$"))
			context.reportOriginForExpression(this, r.singleDeclarationRegionUsed.region(), (IFile) r.singleDeclarationRegionUsed.entityAs(Declaration.class).resource());
		else if (!r.anySubstitutionsApplied)
			context.reportOriginForExpression(this, new SourceLocation(context.codeFragmentOffset(), this), context.script().scriptFile());
		return r.evaluated;
	}

	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		
		// warn about overly long strings
		long max = parser.containingScript().index().engine().settings().maxStringLen;
		if (max != 0 && literal().length() > max) {
			parser.warningWithCode(ParserErrorCode.StringTooLong, this, literal().length(), max);
		}
		
		// stringtbl entries
		// don't warn in #appendto scripts because those will inherit their string tables from the scripts they are appended to
		// and checking for the existence of the table entries there is overkill
		if (parser.hasAppendTo() || parser.containingScript().resource() == null)
			return;
		String value = literal();
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