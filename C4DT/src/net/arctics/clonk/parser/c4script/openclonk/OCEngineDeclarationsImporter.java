package net.arctics.clonk.parser.c4script.openclonk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathExpressionException;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.c4script.XMLDocImporter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.xml.sax.SAXException;

public class OCEngineDeclarationsImporter {

	public void importFromRepository(C4ScriptBase importsContainer, String repository, IProgressMonitor monitor) throws XPathExpressionException, FileNotFoundException, SAXException, IOException {
		XMLDocImporter importer = XMLDocImporter.instance();
		importer.setRepositoryPath(repository);
		String fnFolderPath = repository + "/docs/sdk/script/fn"; //$NON-NLS-1$
		File fnFolder = new File(fnFolderPath);
		String[] files = fnFolder.list(new FilenameFilter() {
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
			C4Declaration declaration;
			try {
				declaration = importer.importFromXML(new FileInputStream(fnFolderPath + "/" + fileName)); //$NON-NLS-1$
			} catch (Exception e) {
				declaration = null;
			}
			if (declaration != null) {
				C4Declaration existing = importsContainer.findDeclaration(declaration.getName(), declaration.getClass());
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
		readMissingFuncsFromSource(importsContainer, repository);
		if (monitor != null)
			monitor.done();
	}
	
	private static final Pattern nillablePattern = Pattern.compile("Nillable\\<(.*?)\\>");
	private static final Pattern pointerTypePattern = Pattern.compile("(.*?)\\s*?\\*");
	
	private static C4Type typeFromCPPType(String type) {
		Matcher m;
		if (type.equals("C4Value")) {
			return C4Type.ANY;
		} else if (type.equals("C4Void")) {
			return C4Type.ANY;
		} else if (type.equals("long")) {
			return C4Type.INT;
		} else if (type.equals("bool")) {
			return C4Type.BOOL;
		} else if (type.equals("C4ID")) {
			return C4Type.ID;
		} else if ((m = nillablePattern.matcher(type)).matches()) {
			return typeFromCPPType(m.group(1));
		} else if ((m = pointerTypePattern.matcher(type)).matches()) {
			String t = m.group(1);
			if (t.equals("C4Object")) {
				return C4Type.OBJECT;
			} else  if (t.equals("C4PropList")) {
				return C4Type.PROPLIST;
			} else if (t.equals("C4Value")) {
				return C4Type.ANY;
			} else if (t.equals("C4String")) {
				return C4Type.STRING;
			} else {
				return C4Type.UNKNOWN;
			}
		} else {
			return C4Type.UNKNOWN;
		}
	}

	private void readMissingFuncsFromSource(C4ScriptBase importsContainer, String repository) throws FileNotFoundException, IOException {

		final int SECTION_None = 0;
		final int SECTION_InitFunctionMap = 1;
		final int SECTION_C4ScriptConstMap = 2;
		final int SECTION_C4ScriptFnMap = 3;
		final int SECTION_FnDeclarations = 4;

		String c4ScriptFilePath = repository + "/src/game/script/C4Script.cpp"; //$NON-NLS-1$
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
							C4Function fun = importsContainer.findLocalFunction(name, false);
							if (fun == null) {
								fun = new C4Function(name, C4Type.ANY);
								List<C4Variable> parms = new ArrayList<C4Variable>(1);
								parms.add(new C4Variable("...", C4Type.ANY)); //$NON-NLS-1$
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
							C4Type type;
							try {
								type = C4Type.makeType(typeString.substring(4).toLowerCase());
							} catch (Exception e) {
								System.out.println(typeString);
								type = C4Type.INT;
							}
							String comment = constMapMatcher.group(5); 

							C4Variable cnst = importsContainer.findLocalVariable(name, false);
							if (cnst == null) {
								cnst = new C4Variable(name, type);
								cnst.setScope(C4VariableScope.CONST);
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
							C4Function fun = importsContainer.findLocalFunction(name, false);
							if (fun == null) {
								fun = new C4Function(name, C4Type.makeType(retType.substring(4).toLowerCase(), true));
								String[] p = parms.split(","); //$NON-NLS-1$
								List<C4Variable> parList = new ArrayList<C4Variable>(p.length);
								for (String pa : p) {
									parList.add(new C4Variable("par"+(parList.size()+1), C4Type.makeType(pa.trim().substring(4).toLowerCase(), true))); //$NON-NLS-1$
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
							C4Function fun = importsContainer.findLocalFunction(name, false);
							if (fun == null) {
								fun = new C4Function(name, typeFromCPPType(returnType));
								String[] parmStrings = parms.split("\\,");
								List<C4Variable> parList = new ArrayList<C4Variable>(parmStrings.length);
								for (String parm : parmStrings) {
									int x;
									for (x = parm.length()-1; x >= 0 && BufferedScanner.isWordPart(parm.charAt(x)); x--);
									String pname = parm.substring(x+1);
									String type = parm.substring(0, x+1).trim();
									parList.add(new C4Variable(pname, typeFromCPPType(type)));
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
		}
	}
}
