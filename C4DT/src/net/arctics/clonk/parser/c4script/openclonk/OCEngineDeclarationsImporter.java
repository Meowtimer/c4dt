package net.arctics.clonk.parser.c4script.openclonk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.XMLDocImporter;
import net.arctics.clonk.parser.c4script.XMLDocImporter.ExtractedDeclarationDocumentation;
import net.arctics.clonk.preferences.ClonkPreferences;

import org.eclipse.core.runtime.IProgressMonitor;

public class OCEngineDeclarationsImporter {

	public void importFromRepository(Script importsContainer, String repository, IProgressMonitor monitor) {
		XMLDocImporter importer = importsContainer.engine().repositoryDocImporter();
		importer.setRepositoryPath(repository);
		String fnFolderPath = repository + "/docs/sdk/script/fn"; //$NON-NLS-1$
		File fnFolder = new File(fnFolderPath);
		String[] files = fnFolder.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".xml"); //$NON-NLS-1$
			}
		});
		if (monitor != null)
			monitor.beginTask("Importing", files.length); //$NON-NLS-1$
		for (String fileName : files) {
			if (monitor != null) {
				monitor.subTask(fileName);
			}
			Declaration declaration;
			try {
				ExtractedDeclarationDocumentation extracted = importer.extractDeclarationInformationFromFunctionXml(fileName, ClonkPreferences.getLanguagePref(), XMLDocImporter.DOCUMENTATION); 
				declaration = extracted != null ? extracted.toDeclaration() : null;
			} catch (Exception e) {
				declaration = null;
			}
			if (declaration != null) {
				Declaration existing = importsContainer.findDeclaration(declaration.name(), declaration.getClass());
				if (existing != null) {
					existing.absorb(declaration);
				} else {
					importsContainer.addDeclaration(declaration);
				}
			}
			if (monitor != null) {
				monitor.worked(1);
			}
		}
		// also import from fn list in C4Script.cpp
		readMissingFuncsFromSource(importsContainer, repository, "/src/game/script/C4Script.cpp");
		readMissingFuncsFromSource(importsContainer, repository, "/src/game/script/C4ObjectScript.cpp");
		if (monitor != null)
			monitor.done();
	}

	private void readMissingFuncsFromSource(Script importsContainer, String repository, String sourceFilePath) {

		final int SECTION_None = 0;
		final int SECTION_InitFunctionMap = 1;
		final int SECTION_C4ScriptConstMap = 2;
		final int SECTION_C4ScriptFnMap = 3;
		final int SECTION_FnDeclarations = 4;

		String c4ScriptFilePath = repository + sourceFilePath; //$NON-NLS-1$
		File c4ScriptFile;
		if ((c4ScriptFile = new File(c4ScriptFilePath)).exists()) {
			Matcher[] sectionStartMatchers = new Matcher[] {
				Pattern.compile("void InitFunctionMap\\(C4AulScriptEngine \\*pEngine\\)").matcher(""), //$NON-NLS-1$ //$NON-NLS-2$
				Pattern.compile("C4ScriptConstDef C4ScriptConstMap\\[\\]\\=\\{").matcher(""), //$NON-NLS-1$ //$NON-NLS-2$
				Pattern.compile("C4ScriptFnDef C4ScriptFnMap\\[\\]\\=").matcher(""), //$NON-NLS-1$ //$NON-NLS-2$
				Pattern.compile(".*?C4Script Functions.*").matcher("")
			};
			Matcher fnMapMatcher = Pattern.compile("\\s*\\{\\s*\"(.*?)\"\\s*,\\s*(.*?)\\s*,\\s*(.*?)\\s*,\\s*\\{(.*?)\\}\\s*,\\s*(.*?)\\s*,\\s*(.*?)\\s*\\}\\s*,").matcher(""); //$NON-NLS-1$ //$NON-NLS-2$
			Matcher constMapMatcher = Pattern.compile("\\s*\\{\\s*\"(.*?)\"\\s*,\\s*(.*?)\\s*,\\s*(.*?)\\s*\\}\\s*,\\s*(\\/\\/(.*))?").matcher(""); //$NON-NLS-1$ //$NON-NLS-2$
			Matcher addFuncMatcher = Pattern.compile("\\s*AddFunc\\s*\\(\\s*pEngine\\s*,\\s*\"(.*?)\"\\s*,\\s*.*?\\)\\s*;").matcher(""); //$NON-NLS-1$ //$NON-NLS-2$
			Matcher fnDeclarationMatcher = Pattern.compile("static (.*?) Fn(.*?)\\(C4Aul(Object)?Context.*?,(.*?)\\)").matcher("");

			try {
				BufferedReader reader = new BufferedReader(new FileReader(c4ScriptFile));
				int section = SECTION_None;
				try {
					String line;
					Outer: while ((line = reader.readLine()) != null) {
						// determine section
						for (int s = 0; s < sectionStartMatchers.length; s++) {
							sectionStartMatchers[s].reset(line);
							if (sectionStartMatchers[s].matches()) {
								section = s+1;
								continue Outer;
							}
						}

						switch (section) {
						case SECTION_InitFunctionMap:
							if (addFuncMatcher.reset(line).matches()) {
								String name = addFuncMatcher.group(1);
								Function fun = importsContainer.findLocalFunction(name, false);
								if (fun == null) {
									fun = new Function(name, PrimitiveType.ANY);
									List<Variable> parms = new ArrayList<Variable>(1);
									parms.add(new Variable("...", PrimitiveType.ANY)); //$NON-NLS-1$
									fun.setParameters(parms);
									importsContainer.addDeclaration(fun);
								}
							}
							break;
						case SECTION_C4ScriptConstMap:
							if (constMapMatcher.reset(line).matches()) {
								int i = 1;
								String name = constMapMatcher.group(i++);
								String typeString = constMapMatcher.group(i++);
								PrimitiveType type;
								try {
									type = PrimitiveType.makeType(typeString.substring(4).toLowerCase());
								} catch (Exception e) {
									System.out.println(typeString);
									type = PrimitiveType.INT;
								}
								String comment = constMapMatcher.group(5); 

								Variable cnst = importsContainer.findLocalVariable(name, false);
								if (cnst == null) {
									cnst = new Variable(name, type);
									cnst.setScope(Scope.CONST);
									cnst.setUserDescription(comment);
									importsContainer.addDeclaration(cnst);
								}
							}
							break;
						case SECTION_C4ScriptFnMap:
							if (fnMapMatcher.reset(line).matches()) {
								int i = 1;
								String name = fnMapMatcher.group(i++);
								i++;//String public_ = fnMapMatcher.group(i++);
								String retType = fnMapMatcher.group(i++);
								String parms = fnMapMatcher.group(i++);
								//String pointer = fnMapMatcher.group(i++);
								//String oldPointer = fnMapMatcher.group(i++);
								Function fun = importsContainer.findLocalFunction(name, false);
								if (fun == null) {
									fun = new Function(name, PrimitiveType.makeType(retType.substring(4).toLowerCase(), true));
									String[] p = parms.split(","); //$NON-NLS-1$
									List<Variable> parList = new ArrayList<Variable>(p.length);
									for (String pa : p) {
										parList.add(new Variable("par"+(parList.size()+1), PrimitiveType.makeType(pa.trim().substring(4).toLowerCase(), true))); //$NON-NLS-1$
									}
									fun.setParameters(parList);
									importsContainer.addDeclaration(fun);
								}
							}
							break;
						case SECTION_FnDeclarations:
							if (fnDeclarationMatcher.reset(line).matches()) {
								int i = 1;
								String returnType = fnDeclarationMatcher.group(i++);
								String name = fnDeclarationMatcher.group(i++);
								// some functions to be ignored
								System.out.println(name);
								if (name.equals("_goto") || name.equals("_this")) {
									System.out.println("continue!1");
									continue;
								}
								i++; // optional Object in C4AulContext
								String parms = fnDeclarationMatcher.group(i++);
								Function fun = importsContainer.findLocalFunction(name, false);
								if (fun == null) {
									fun = new Function(name, PrimitiveType.typeFromCPPType(returnType));
									String[] parmStrings = parms.split("\\,");
									List<Variable> parList = new ArrayList<Variable>(parmStrings.length);
									for (String parm : parmStrings) {
										int x;
										for (x = parm.length()-1; x >= 0 && BufferedScanner.isWordPart(parm.charAt(x)); x--);
										String pname = parm.substring(x+1);
										String type = parm.substring(0, x+1).trim();
										parList.add(new Variable(pname, PrimitiveType.typeFromCPPType(type)));
									}
									fun.setParameters(parList);
									importsContainer.addDeclaration(fun);
								}
							}
							break;
						}
					}
				}
				finally {
					reader.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
