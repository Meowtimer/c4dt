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
import net.arctics.clonk.parser.c4script.SpecialEngineRules;
import net.arctics.clonk.parser.c4script.SpecialEngineRules.SpecialFuncRule;
import net.arctics.clonk.parser.c4script.ast.evaluate.IEvaluationContext;
import net.arctics.clonk.parser.stringtbl.StringTbl;
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
	public void doPrint(ExprWriter output, int depth) {
		output.append("\""); //$NON-NLS-1$
		output.append(stringValue());
		output.append("\""); //$NON-NLS-1$
	}

	@Override
	public IType unresolvedType(DeclarationObtainmentContext context) {
		return PrimitiveType.STRING;
	}

	@Override
	public EntityRegion entityAt(int offset, C4ScriptParser parser) {

		// first check if a string tbl entry is referenced
		EntityRegion result = StringTbl.entryForLanguagePref(stringValue(), start(), (offset-1), parser.script(), true);
		if (result != null)
			return result;

		// look whether some special linking rule can be applied to this literal
		if (parent() instanceof CallDeclaration) {
			CallDeclaration parentFunc = (CallDeclaration) parent();
			int myIndex = parentFunc.indexOfParm(this);

			// delegate finding a link to special function rules
			SpecialFuncRule funcRule = parentFunc.specialRuleFromContext(parser, SpecialEngineRules.DECLARATION_LOCATOR);
			if (funcRule != null) {
				EntityRegion region = funcRule.locateEntityInParameter(parentFunc, parser, myIndex, offset, this);
				if (region != null)
					return region;
			}

		}
		return super.entityAt(offset, parser);
	}
	
	@Override
	public String evaluateAtParseTime(IEvaluationContext context) {
		String escapesEvaluated = StringUtil.evaluateEscapes(literal());
		if (context == null || context.script() == null)
			return escapesEvaluated;
		StringTbl.EvaluationResult r = StringTbl.evaluateEntries(context.script(), escapesEvaluated, false);
		// getting over-the-top: trace back to entry in StringTbl file to which the literal needs to be completely evaluated to 
		if (r.singleDeclarationRegionUsed != null && literal().matches("\\$.*?\\$"))
			context.reportOriginForExpression(this, r.singleDeclarationRegionUsed.region(), (IFile) r.singleDeclarationRegionUsed.entityAs(Declaration.class).resource());
		else if (!r.anySubstitutionsApplied)
			context.reportOriginForExpression(this, new SourceLocation(context.codeFragmentOffset(), this), context.script().scriptFile());
		return r.evaluated;
	}

	@Override
	public void reportProblems(C4ScriptParser parser) throws ParsingException {
		
		// warn about overly long strings
		long max = parser.script().index().engine().settings().maxStringLen;
		if (max != 0 && literal().length() > max)
			parser.warning(ParserErrorCode.StringTooLong, this, literal().length(), max);
		
		// stringtbl entries
		// don't warn in #appendto scripts because those will inherit their string tables from the scripts they are appended to
		// and checking for the existence of the table entries there is overkill
		if (parser.hasAppendTo() || parser.script().resource() == null)
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