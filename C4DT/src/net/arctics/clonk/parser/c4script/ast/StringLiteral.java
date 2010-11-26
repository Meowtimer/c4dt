package net.arctics.clonk.parser.c4script.ast;

import java.util.regex.Matcher;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4Scenario;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.stringtbl.StringTbl;
import net.arctics.clonk.ui.editors.c4script.ExpressionLocator;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public final class StringLiteral extends Literal<String> {
	/**
	 * 
	 */
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
	public IType getType(C4ScriptParser context) {
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

			// GameCall: look for nearest scenario and find function in its script
			if (myIndex == 0 && parentFunc.getDeclaration() == getCachedFuncs(parser).GameCall) {
				ClonkIndex index = parser.getContainer().getIndex();
				C4Scenario scenario = ClonkIndex.pickNearest(parser.getContainer().getResource(), index.getIndexedScenarios());
				if (scenario != null) {
					C4Function scenFunc = scenario.findFunction(stringValue());
					if (scenFunc != null)
						return new DeclarationRegion(scenFunc, identifierRegion());
				}
			}
			
			else if (myIndex == 0 && parentFunc.getDeclarationName().equals("Schedule")) { //$NON-NLS-1$
				// parse first parm of Schedule as expression and see what goes
				ExpressionLocator locator = new ExpressionLocator(offset-1); // make up for '"'
				try {
					C4ScriptParser.parseStandaloneStatement(getLiteral(), parser.getActiveFunc(), locator);
				} catch (ParsingException e) {}
				if (locator.getExprAtRegion() != null) {
					DeclarationRegion reg = locator.getExprAtRegion().declarationAt(offset, parser);
					if (reg != null)
						return reg.addOffsetInplace(getExprStart()+1);
					else
						return null;
				}
				else
					return super.declarationAt(offset, parser);	
			}

			// LocalN: look for local var in object
			else if (myIndex == 0 && parentFunc.getDeclaration() == getCachedFuncs(parser).LocalN) {
				C4Object typeToLookIn = parentFunc.getParams().length > 1 ? parentFunc.getParams()[1].guessObjectType(parser) : null;
				if (typeToLookIn == null && parentFunc.getPredecessorInSequence() != null)
					typeToLookIn = parentFunc.getPredecessorInSequence().guessObjectType(parser);
				if (typeToLookIn == null)
					typeToLookIn = parser.getContainerObject();
				if (typeToLookIn != null) {
					C4Variable var = typeToLookIn.findVariable(stringValue());
					if (var != null)
						return new DeclarationRegion(var, identifierRegion());
				}
			}

			// look for function called by Call("...")
			else if (myIndex == 0 && parentFunc.getDeclaration() == getCachedFuncs(parser).Call) {
				C4Function f = parser.getContainer().findFunction(stringValue());
				if (f != null)
					return new DeclarationRegion(f, identifierRegion());
			}

			// ProtectedCall/PrivateCall/ObjectCall, a bit more complicated than Call
			else if (myIndex == 1 && (Utilities.isAnyOf(parentFunc.getDeclaration(), getCachedFuncs(parser).ObjectCallFunctions) || parentFunc.getDeclarationName().equals("ScheduleCall"))) { //$NON-NLS-1$
				C4Object typeToLookIn = parentFunc.getParams()[0].guessObjectType(parser);
				if (typeToLookIn == null && parentFunc.getPredecessorInSequence() != null)
					typeToLookIn = parentFunc.getPredecessorInSequence().guessObjectType(parser);
				if (typeToLookIn == null)
					typeToLookIn = parser.getContainerObject();
				if (typeToLookIn != null) {
					C4Function f = typeToLookIn.findFunction(stringValue());
					if (f != null)
						return new DeclarationRegion(f, identifierRegion());
				} 
			}

		}
		return null;
	}
	
	@Override
	public String evaluateAtParseTime(C4ScriptBase context) {
		String value = getLiteral().replaceAll("\\\"", "\"");
		return StringTbl.evaluateEntries(context, value, getExprStart());
	}

	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		
		// warn about overly long strings
		int max = parser.getContainer().getIndex().getEngine().getCurrentSettings().maxStringLen;
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