package net.arctics.clonk.parser.c4script;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.index.DocumentedFunction;
import net.arctics.clonk.index.DocumentedVariable;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Engine.EngineSettings;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.util.StreamUtil;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Helper to import declarations from source files. Geared towards OpenClonk source.
 * @author madeen
 *
 */
public class CPPSourceDeclarationsImporter {

	public boolean overwriteExistingDeclarations = true;
	
	/**
	 * Import declarations from a source repository, putting them in the supplied {@link Script} container.
	 * Source file paths are fetched from the container's engine's settings' {@link EngineSettings#cppSources} 
	 * @param importsContainer The container to import the declarations into. Usually an {@link Engine}
	 * @param repository Repository path to prepend to cpp source paths
	 * @param monitor Monitor used to monitor the progress of the operation.
	 */
	public void importFromRepository(Script importsContainer, String repository, IProgressMonitor monitor) {
		// also import from fn list in C4Script.cpp
		for (String sourceFile : importsContainer.engine().currentSettings().cppSources.split(","))
			readDeclarationsFromSource(importsContainer, repository, sourceFile);
		if (monitor != null)
			monitor.done();
	}

	private void readDeclarationsFromSource(Script importsContainer, String repository, String sourceFilePath) {

		String origin = sourceFilePath;
		
		final int SECTION_None = 0;
		final int SECTION_InitFunctionMap = 1;
		final int SECTION_C4ScriptConstMap = 2;
		final int SECTION_C4ScriptFnMap = 3;

		String c4ScriptFilePath = repository + sourceFilePath; //$NON-NLS-1$
		File c4ScriptFile;
		EngineSettings settings = importsContainer.engine().currentSettings();
		if ((c4ScriptFile = new File(c4ScriptFilePath)).exists()) {
			Matcher[] sectionStartMatchers = new Matcher[] {
				Pattern.compile(settings.initFunctionMapPattern).matcher(""),
				Pattern.compile(settings.constMapPattern).matcher(""),
				Pattern.compile(settings.fnMapPattern).matcher("")
			};
			Matcher fnMapEntryMatcher = Pattern.compile(settings.fnMapEntryPattern).matcher(""); //$NON-NLS-1$ //$NON-NLS-2$
			Matcher constMapEntryMatcher = Pattern.compile(settings.constMapEntryPattern).matcher(""); //$NON-NLS-1$ //$NON-NLS-2$
			Matcher addFuncMatcher = Pattern.compile(settings.addFuncPattern).matcher(""); //$NON-NLS-1$ //$NON-NLS-2$
			Matcher fnDeclarationMatcher = Pattern.compile(settings.fnDeclarationPattern).matcher("");

			try {
				String sourceContents = StreamUtil.stringFromFile(c4ScriptFile);
				BufferedScanner scanner = new BufferedScanner(sourceContents);
				int section = SECTION_None;
				int lineOffset = 0;
				Outer: for (String line = scanner.readLine(); !scanner.reachedEOF(); lineOffset = scanner.tell(), line = scanner.readLine()) {
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
								fun = new DocumentedFunction(name, PrimitiveType.ANY, origin);
								fun.setLocation(new SourceLocation(lineOffset, lineOffset+line.length()));
								List<Variable> parms = new ArrayList<Variable>(1);
								parms.add(new Variable("...", PrimitiveType.ANY)); //$NON-NLS-1$
								fun.setParameters(parms);
								importsContainer.addDeclaration(fun);
							}
							continue Outer;
						}
						break;
					case SECTION_C4ScriptConstMap:
						if (constMapEntryMatcher.reset(line).matches()) {
							int i = 1;
							String name = constMapEntryMatcher.group(i++);
							String typeString = constMapEntryMatcher.group(i++);
							PrimitiveType type;
							try {
								type = PrimitiveType.makeType(typeString.substring(4).toLowerCase());
							} catch (Exception e) {
								type = PrimitiveType.INT;
							}

							Variable cnst = importsContainer.findLocalVariable(name, false);
							if (cnst == null) {
								cnst = new DocumentedVariable(name, type);
								cnst.setScope(Scope.CONST);
								importsContainer.addDeclaration(cnst);
							}
							continue Outer;
						}
						break;
					case SECTION_C4ScriptFnMap:
						if (fnMapEntryMatcher.reset(line).matches()) {
							int i = 1;
							String name = fnMapEntryMatcher.group(i++);
							i++;//String public_ = fnMapMatcher.group(i++);
							String retType = fnMapEntryMatcher.group(i++);
							String parms = fnMapEntryMatcher.group(i++);
							//String pointer = fnMapMatcher.group(i++);
							//String oldPointer = fnMapMatcher.group(i++);
							Function fun = importsContainer.findLocalFunction(name, false);
							if (fun == null) {
								fun = new DocumentedFunction(name, PrimitiveType.makeType(retType.substring(4).toLowerCase(), true), origin);
								fun.setLocation(new SourceLocation(lineOffset, lineOffset+line.length()));
								String[] p = parms.split(","); //$NON-NLS-1$
								List<Variable> parList = new ArrayList<Variable>(p.length);
								for (String pa : p) {
									parList.add(new Variable("par"+(parList.size()+1), PrimitiveType.makeType(pa.trim().substring(4).toLowerCase(), true))); //$NON-NLS-1$
								}
								fun.setParameters(parList);
								importsContainer.addDeclaration(fun);
							}
							continue Outer;
						}
						break;
					}
					
					if (fnDeclarationMatcher.reset(line).matches()) {
						int i = 1;
						String returnType = fnDeclarationMatcher.group(i++);
						String name = fnDeclarationMatcher.group(i++);
						// some functions to be ignored
						if (name.equals("_goto") || name.equals("_this")) {
							continue;
						}
						i++; // optional Object in C4AulContext
						i++; // optional actual parameters with preceding comma
						String parms = fnDeclarationMatcher.group(i++);
						Function fun = importsContainer.findLocalFunction(name, false);
						if (fun == null) {
							fun = new DocumentedFunction(name, PrimitiveType.typeFromCPPType(returnType), origin);
							fun.setLocation(new SourceLocation(lineOffset, lineOffset+line.length()));
							String[] parmStrings = parms != null ? parms.split("\\,") : null;
							List<Variable> parList = new ArrayList<Variable>(parmStrings != null ? parmStrings.length : 0);
							if (parmStrings != null) {
								for (String parm : parmStrings) {
									int x;
									for (x = parm.length()-1; x >= 0 && BufferedScanner.isWordPart(parm.charAt(x)); x--);
									String pname = parm.substring(x+1);
									String type = parm.substring(0, x+1).trim();
									parList.add(new Variable(pname, PrimitiveType.typeFromCPPType(type)));
								}
							}
							fun.setParameters(parList);
							importsContainer.addDeclaration(fun);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
