package net.arctics.clonk.parser.c4script.ast;

import java.util.regex.Matcher;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.SpecialScriptRules.SpecialFuncRule;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

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
	protected IType obtainType(C4ScriptParser context) {
		return C4Type.STRING;
	}

	@Override
	public DeclarationRegion declarationAt(int offset, C4ScriptParser parser) {

		// first check if a string tbl entry is referenced
		DeclarationRegion result = StringTbl.getEntryForLanguagePref(stringValue(), getExprStart(), (offset-1), parser.getContainer(), true);
		if (result != null)
			return result;

		// look whether this string can be considered a function name
		if (getParent() instanceof CallFunc) {
			CallFunc parentFunc = (CallFunc) getParent();
			int myIndex = parentFunc.indexOfParm(this);

			//  link to functions that are called indirectly
			
			SpecialFuncRule funcRule = parentFunc.getSpecialRule(parser);
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
	public String evaluateAtParseTime(C4ScriptBase context) {
		String value = getLiteral().replaceAll("\\\"", "\"");
		return StringTbl.evaluateEntries(context, value, getExprStart());
	}

	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		
		// warn about overly long strings
		long max = parser.getContainer().getIndex().getEngine().getCurrentSettings().maxStringLen;
		if (max != 0 && getLiteral().length() > max) {
			parser.warningWithCode(ParserErrorCode.StringTooLong, this, getLiteral().length(), max);
		}
		
		// stringtbl entries
		// don't warn in #appendto scripts because those will inherit their string tables from the scripts they are appended to
		// and checking for the existence of the table entries there is overkill
		if (parser.hasAppendTo() || parser.getContainer().getResource() == null)
			return;
		String value = getLiteral();
		int valueLen = value.length();
		// warn when using non-declared string tbl entries
		for (int i = 0; i < valueLen;) {
			if (i+1 < valueLen && value.charAt(i) == '$') {
				DeclarationRegion region = StringTbl.getEntryRegion(stringValue(), getExprStart(), (i+1));
				if (region != null) {
					StringBuilder listOfLangFilesItsMissingIn = null;
					try {
						for (IResource r : (parser.getContainer().getResource() instanceof IContainer ? (IContainer)parser.getContainer().getResource() : parser.getContainer().getResource().getParent()).members()) {
							if (!(r instanceof IFile))
								continue;
							IFile f = (IFile) r;
							Matcher m = StringTbl.PATTERN.matcher(r.getName());
							if (m.matches()) {
								String lang = m.group(1);
								StringTbl tbl = (StringTbl)StringTbl.pinned(f, true, false);
								if (tbl != null) {
									if (tbl.getMap().get(region.getText()) == null) {
										if (listOfLangFilesItsMissingIn == null)
											listOfLangFilesItsMissingIn = new StringBuilder(10);
										if (listOfLangFilesItsMissingIn.length() > 0)
											listOfLangFilesItsMissingIn.append(", "); //$NON-NLS-1$
										listOfLangFilesItsMissingIn.append(lang);
									}
								}
							}
						}
					} catch (CoreException e) {}
					if (listOfLangFilesItsMissingIn != null) {
						parser.warningWithCode(ParserErrorCode.MissingLocalizations, region.getRegion(), listOfLangFilesItsMissingIn.toString());
					}
					i += region.getRegion().getLength();
					continue;
				}
			}
			++i;
		}
	}

	@Override
	public int getIdentifierStart() {
		return getExprStart()+1;
	}

	@Override
	public int getIdentifierLength() {
		return stringValue().length();
	}

}