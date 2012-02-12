package net.arctics.clonk.index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IHasName;
import net.arctics.clonk.parser.c4script.IHasUserDescription;
import net.arctics.clonk.parser.c4script.ITypeable;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.SpecialScriptRules;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.XMLDocImporter;
import net.arctics.clonk.parser.c4script.XMLDocImporter.ExtractedDeclarationDocumentation;
import net.arctics.clonk.parser.c4script.openclonk.OCSourceDeclarationsImporter;
import net.arctics.clonk.parser.inireader.CustomIniUnit;
import net.arctics.clonk.parser.inireader.IniData;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.parser.inireader.IniField;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IStorageLocation;
import net.arctics.clonk.util.LineNumberObtainer;
import net.arctics.clonk.util.SettingsBase;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * Container for engine functions and constants.
 * @author Madeen
 *
 */
public class Engine extends Script {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public static class EngineSettings extends SettingsBase {

		/** Default strictness level applied to scripts with no explicit #strict line. */
		@IniField(category="Intrinsic")
		public long strictDefaultLevel;
		/** Maximum string length of string constants. */
		@IniField(category="Intrinsic")
		public long maxStringLen;
		/** Whether engine supports colon ID syntax (:Clonk, :Firestone). Enforcing this syntax was discussed and then dropped. */
		@IniField(category="Intrinsic")
		public boolean colonIDSyntax;
		/**
		 * Whether declarations of static non-const variables are allowed to include an assignment. OC added support for this.
		 */
		@IniField(category="Intrinsic")
		public boolean nonConstGlobalVarsAssignment;
		/**
		 * HACK: In OC, object definition constants (Clonk, Firestone) actually are parsed as referring to a Variable object each Definition maintains as its 'proxy variable'.<br/>
		 * This toggle activates/deactivates this behaviour.
		 * */ 
		@IniField(category="Intrinsic")
		public boolean definitionsHaveProxyVariables;
		/** Whether engine supports ref parameters (int & x). OpenClonk stopped supporting it. */
		@IniField(category="Intrinsic")
		public boolean supportsRefs;
		/** Whether engine supports creating a debug connection and single-stepping through C4Script code */
		@IniField(category="Intrinsic")
		public boolean supportsDebugging;
		/** Name of editor mode option (CR: console, OC: editor) */
		@IniField(category="Intrinsic")
		public String editorCmdLineOption;
		/** Format for commandline options (ClonkRage: /%s vs OpenClonk: --%s) */
		@IniField(category="Intrinsic")
		public String cmdLineOptionFormat;
		/** Format for commandline options that take an argument --%s=%s */
		@IniField(category="Intrinsic")
		public String cmdLineOptionWithArgumentFormat;
		/** Whether engine supports -x <command> */
		@IniField(category="Intrinsic")
		public boolean supportsEmbeddedUtilities;
		/** Whether engine parser allows obj-> ~DoSomething() */
		@IniField(category="Intrinsic")
		public boolean spaceAllowedBetweenArrowAndTilde;
		/** String of the form c4d->DefinitionGroup,... specifying what file extension denote what group type. */
		@IniField(category="Intrinsic")
		public String fileExtensionToGroupTypeMapping;
		/** Extension for material definition files */
		@IniField(category="Intrinsic")
		public String materialExtension;
		/** Whether 0 is of type any */
		@IniField(category="Intrinsic")
		public boolean treatZeroAsAny;
		/** Engine supports proplists (OC) */
		@IniField(category="Intrinsic")
		public boolean proplistsSupported;
		/** ';'-separated list of file extensions supported as sound files */
		@IniField(category="Intrinsic")
		public String supportedSoundFileExtensions;
		
		// Settings that are actually intended to be user-configurable
		
		/** Template for Documentation URL. */
		@IniField
		public String docURLTemplate;
		@IniField
		public boolean useDocsFromRepository;
		/** Path to engine executable. */
		@IniField
		public String engineExecutablePath;
		/** Path to game folder. */
		@IniField
		public String gamePath;
		/** Path to OC repository. To be used for automatically importing engine definitions (FIXME: needs proper implementation). */
		@IniField
		public String repositoryPath;
		/** Path to c4group executable */
		@IniField
		public String c4GroupPath;
		/** Whether to dynamically load documentation from the given repository **/
		@IniField
		public boolean readDocumentationFromRepository;
		
		@IniField(category="Source")
		public String initFunctionMapPattern;
		@IniField(category="Source")
		public String constMapPattern;
		@IniField(category="Source")
		public String fnMapPattern;
		@IniField(category="Source")
		public String fnMapEntryPattern;
		@IniField(category="Source")
		public String constMapEntryPattern;
		@IniField(category="Source")
		public String addFuncPattern;
		@IniField(category="Source")
		public String fnDeclarationPattern;
		
		private transient Map<String, C4Group.GroupType> fetgtm;
		private transient Map<C4Group.GroupType, String> rfetgtm;
		private transient List<String> supportedSoundFileExtensions_;
		
		/**
		 * Return a map mapping a file name extension to a group type for this engine.
		 * @return The map.
		 */
		public Map<String, C4Group.GroupType> fileExtensionToGroupTypeMapping() {
			if (fetgtm == null) {
				fetgtm = new HashMap<String, C4Group.GroupType>(C4Group.GroupType.values().length);
				for (String mapping : fileExtensionToGroupTypeMapping.split(",")) {
					String[] elms = mapping.split("->");
					if (elms.length >= 2) {
						C4Group.GroupType gt = C4Group.GroupType.valueOf(elms[1]);
						fetgtm.put(elms[0], gt);
					}
				}
				rfetgtm = ArrayUtil.reverseMap(fetgtm, new HashMap<C4Group.GroupType, String>());
			}
			return fetgtm;
		}
		
		public Map<C4Group.GroupType, String> groupTypeToFileExtensionMapping() {
			fileExtensionToGroupTypeMapping();
			return rfetgtm;
		}
		
		private transient String fileDialogFilterString;
		/**
		 * Return a filter string for c4group files to be used with file dialogs
		 * @return The filter string
		 */
		public String fileDialogFilterForGroupFiles() {
			if (fileDialogFilterString == null) {
				StringBuilder builder = new StringBuilder(6*fileExtensionToGroupTypeMapping().size());
				for (String ext : fileExtensionToGroupTypeMapping().keySet()) {
					builder.append("*.");
					builder.append(ext);
					builder.append(";");
				}
				fileDialogFilterString = builder.toString();
			}
			return fileDialogFilterString;
		}

		public String documentationURLForFunction(String functionName) {
			String urlFormatString = useDocsFromRepository
				? "file://" + repositoryPath + "/docs/sdk/script/fn/%1$s.xml"
				: docURLTemplate;
			return String.format(urlFormatString, functionName, ClonkPreferences.getLanguagePrefForDocumentation());
		}
		
		public List<String> supportedSoundFileExtensions() {
			if (supportedSoundFileExtensions_ == null) {
				supportedSoundFileExtensions_ = Arrays.asList(supportedSoundFileExtensions.split("\\;"));
			}
			return supportedSoundFileExtensions_;
		}

	}

	private transient CachedEngineDeclarations cachedFuncs;
	private transient Map<String, Variable[]> cachedPrefixedVariables;

	private transient EngineSettings intrinsicSettings;
	private transient EngineSettings currentSettings;
	
	private transient IStorageLocation[] storageLocations;
	private transient IniData iniConfigurations;
	
	private transient SpecialScriptRules specialScriptRules;
	
	public SpecialScriptRules specialScriptRules() {
		return specialScriptRules;
	}

	public EngineSettings intrinsicSettings() {
		return intrinsicSettings;
	}

	public void setCurrentSettings(EngineSettings currentSettings) {
		EngineSettings oldSettings = this.currentSettings;
		this.currentSettings = currentSettings;
		saveSettings();
		if (
			!Utilities.objectsEqual(oldSettings.repositoryPath, currentSettings.repositoryPath) ||
			oldSettings.readDocumentationFromRepository != currentSettings.readDocumentationFromRepository
		) {
			if (currentSettings.readDocumentationFromRepository)
				reinitializeDocImporter();
			else
				parseEngineScript();
		}
	}

	public EngineSettings currentSettings() {
		return currentSettings;
	}

	public final CachedEngineDeclarations cachedFuncs() {
		return cachedFuncs;
	}
	
	public IniData iniConfigurations() {
		return iniConfigurations;
	}

	public Engine(String name) {
		super(null);
		setName(name);
		modified();
	}

	public void modified() {
		if (intrinsicSettings == null) {
			intrinsicSettings = new EngineSettings();
		}
		cachedFuncs = new CachedEngineDeclarations(this);
		cachedPrefixedVariables = null;
	}

	@Override
	public void postLoad(Declaration parent, Index root) {
		super.postLoad(parent, root);
		modified();
	}

	public static String[] possibleEngineNamesAccordingToOS() {
		// reordered to save lots of cpu time
		if (Util.isWindows()) {
			return new String[] { "Clonk.c4x", "Clonk.exe" }; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (Util.isLinux()) {
			return new String[] { "clonk" }; //$NON-NLS-1$
		}
		if (Util.isMac()) {
			return new String[] {
				"clonk.app/Contents/MacOS/clonk",
				"Clonk.app/Contents/MacOS/Clonk"
			}; 
		}
		// assume some UNIX -.-
		return new String[] { "clonk" }; //$NON-NLS-1$
	}

	public boolean acceptsId(String text) {
		return specialScriptRules().parseId(new BufferedScanner(text)) != null;
	}

	public boolean hasCustomSettings() {
		return !currentSettings.equals(intrinsicSettings);
	}

	@Override
	public Engine engine() {
		return this;
	}

	@Override
	public IFile scriptStorage() {
		return null;
	}
	
	private static final String CONFIGURATION_INI_NAME = "configuration.ini"; //$NON-NLS-1$
	
	public void loadSettings() throws IOException {
		// combine settings files in reverse-order so custom config is based on default
		for (int i = storageLocations.length-1; i >= 0; i--) {
			IStorageLocation loc = storageLocations[i];
			if (loc == null)
				continue;
			URL settingsFile = loc.getURL(CONFIGURATION_INI_NAME, false);
			if (settingsFile != null) {
				InputStream input = settingsFile.openStream();
				try {
					if (currentSettings == null) {
						intrinsicSettings = SettingsBase.createFrom(EngineSettings.class, input);
						currentSettings = (EngineSettings) intrinsicSettings.clone();
					} else {
						currentSettings.loadFrom(input);
					}
				} finally {
					input.close();
				}
			}
		}
		if (intrinsicSettings == null) {
			intrinsicSettings = new EngineSettings();
			currentSettings = (EngineSettings) intrinsicSettings.clone();
		}
	}
	
	private void loadIniConfigurations() {
		try {
			for (int i = storageLocations.length-1; i >= 0; i--) {
				IStorageLocation loc = storageLocations[i];
				if (loc == null)
					continue;
				URL confFile = loc.getURL("iniconfig.xml", false); //$NON-NLS-1$
				if (confFile != null) {
					InputStream input = confFile.openStream();
					try {
						IniData data = new IniData(input);
						data.parse();
						iniConfigurations = data; // set this variable when parsing completed successfully
					} finally {
						input.close();
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class DeclarationsConfiguration extends IniConfiguration {
		@Override
		public boolean hasSection(String sectionName) {
			return Engine.this.findDeclaration(sectionName) != null;
		}
	}

	private final Set<String> namesOfDeclarationsForWhichDocsWereFreshlyObtained = new HashSet<String>();

	private void createPlaceholderDeclarationsToBeFleshedOutFromDocumentation() {
		this.clearDeclarations();
		try {
			createDeclarationsFromRepositoryDocumentationFiles();
			OCSourceDeclarationsImporter importer = new OCSourceDeclarationsImporter();
			importer.overwriteExistingDeclarations = false;
			importer.importFromRepository(this, currentSettings().repositoryPath, new NullProgressMonitor());
			if (findFunction("this") == null)
				this.addDeclaration(new Function("this", Function.FunctionScope.GLOBAL));
			if (findVariable("nil") == null)
				this.addDeclaration(new Variable("nil", Variable.Scope.CONST));
		} finally {
			modified();
		}
	}

	private void createDeclarationsFromRepositoryDocumentationFiles() {
		for (File xmlFile : new File(currentSettings().repositoryPath+"/docs/sdk/script/fn").listFiles()) {
			boolean isConst = false;
			try {
				FileReader r = new FileReader(xmlFile);
				try {
					for (String l : StringUtil.lines(r)) {
						if (l.contains("<const>")) {
							isConst = true;
							break;
						} else if (l.contains("<func>")) {
							isConst = false;
							break;
						}
					}
				} finally {
					r.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			String rawFileName = StringUtil.rawFileName(xmlFile.getName());
			if (isConst)
				this.addDeclaration(new DocumentedVariable(rawFileName, Variable.Scope.CONST));
			else
				this.addDeclaration(new DocumentedFunction(rawFileName, Function.FunctionScope.GLOBAL));
		}
	}
	
	/**
	 * Tell this engine to discard information about documentation already read on-demand from the repository docs folder so
	 * the next time documentation needs to be obtained via {@link #getDescriptionPossiblyReadingItFromRepositoryDocs(IHasUserDescription)},
	 * a repository doc reading will be performed.
	 */
	public void reinitializeDocImporter() {
		xmlDocImporter.discardInitialization();
		if (currentSettings().readDocumentationFromRepository) {
			xmlDocImporter.setRepositoryPath(currentSettings().repositoryPath);
			namesOfDeclarationsForWhichDocsWereFreshlyObtained.clear();
			
			xmlDocImporter.initialize();
			createPlaceholderDeclarationsToBeFleshedOutFromDocumentation();
		}
	}
	
	public <T extends IHasUserDescription & IHasName> String getDescriptionPossiblyReadingItFromRepositoryDocs(T declaration) {
		if (declaration.getCurrentlySetUserDescription() != null && namesOfDeclarationsForWhichDocsWereFreshlyObtained.contains(declaration.name()))
			return declaration.getCurrentlySetUserDescription();
		applyDocumentationAndSignatureFromRepository(declaration);
		return declaration.getCurrentlySetUserDescription();
	}
	
	public <T extends IHasUserDescription & IHasName> boolean applyDocumentationAndSignatureFromRepository(T declaration) {
		namesOfDeclarationsForWhichDocsWereFreshlyObtained.add(declaration.name());
		// dynamically load from repository
		if (currentSettings().readDocumentationFromRepository) {
			XMLDocImporter importer = repositoryDocImporter().initialize();
			ExtractedDeclarationDocumentation d = importer.extractDeclarationInformationFromFunctionXml(declaration.name(), ClonkPreferences.getLanguagePref(), XMLDocImporter.DOCUMENTATION);
			if (d != null) {
				declaration.setUserDescription(d.description);
				if (declaration instanceof Function) {
					Function f = (Function)declaration;
					if (d.parameters != null)
						f.setParameters(d.parameters);
				}
				if (d.returnType != null && declaration instanceof ITypeable)
					((ITypeable)declaration).forceType(d.returnType);
				return true;
			}
		}
		// fallback to description inis
		if (iniDescriptionsLoader == null)
			iniDescriptionsLoader = new IniDescriptionsLoader(this);
		String iniDescription = iniDescriptionsLoader.descriptionFor(declaration);
		if (iniDescription != null)
			declaration.setUserDescription(iniDescription);
		return false;
	}
	
	public void loadDeclarationsConfiguration() {
		for (int i = storageLocations.length-1; i >= 0; i--) {
			IStorageLocation loc = storageLocations[i];
			if (loc == null)
				continue;
			URL url = loc.getURL("declarations.ini", false);
			if (url != null) {
				InputStream stream;
				try {
					stream = url.openStream();
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
				try {
					CustomIniUnit unit = new CustomIniUnit(stream, new DeclarationsConfiguration());
					unit.parser().parse(false);
					for (IniSection section : unit.sections()) {
						Declaration declaration = findDeclaration(section.name());
						if (declaration != null) {
							unit.commitSection(declaration, section, false);
						}
					}
				} finally {
					try {
						stream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void parseEngineScript() {
		for (IStorageLocation loc : storageLocations) {
			final URL url = loc.getURL(name()+".c", false);
			if (url != null) {
				InputStream stream;
				try {
					stream = url.openStream();
				} catch (IOException e1) {
					e1.printStackTrace();
					continue;
				}
				try {
					String scriptFromStream = StreamUtil.stringFromInputStream(stream);
					final LineNumberObtainer lno = new LineNumberObtainer(scriptFromStream);
					C4ScriptParser parser = new C4ScriptParser(scriptFromStream, this, null) {
						private boolean firstMessage = true;
						@Override
						public void markerWithCode(ParserErrorCode code,
								int errorStart, int errorEnd, int flags,
								int severity, Object... args) throws ParsingException {
							if (firstMessage) {
								firstMessage = false;
								System.out.println("Messages while parsing " + url.toString());
							}
							System.out.println(String.format(
									"%s @(%d, %d)",
									code.getErrorString(args),
									lno.obtainLineNumber(errorStart),
									lno.obtainCharNumberInObtainedLine()
									));
							super.markerWithCode(code, errorStart, errorEnd, flags, severity, args);
						}
					};
					try {
						parser.parse();
					} catch (ParsingException e) {
						e.printStackTrace();
					}
					postLoad(null, null);
				} finally {
					try {
						stream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void createSpecialRules() {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends SpecialScriptRules> rulesClass = (Class<? extends SpecialScriptRules>) Engine.class.getClassLoader().loadClass(
				String.format("%s.parser.c4script.specialscriptrules.SpecialScriptRules_%s", ClonkCore.PLUGIN_ID, name()));
			specialScriptRules = rulesClass.newInstance();
			specialScriptRules.initialize();
		} catch (ClassNotFoundException e) {
			// ignore
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Engine loadFromStorageLocations(final IStorageLocation... providers) {
		Engine result = null;
		try {
			for (IStorageLocation location : providers) {
				if (location == null)
					continue;
				// only consider valid engine folder if configuration.ini is present
				URL url = location.getURL(CONFIGURATION_INI_NAME, false); 
				if (url != null) {
					result = new Engine(location.name());
					result.load(providers);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private void load(final IStorageLocation... providers) {
		this.storageLocations = providers;
		try {
			loadSettings();
		} catch (IOException e) {
			e.printStackTrace();
		}
		loadIniConfigurations();
		createSpecialRules();
		if (!currentSettings().readDocumentationFromRepository)
			parseEngineScript();
		loadDeclarationsConfiguration();
		reinitializeDocImporter();
	}

	public void writeEngineScript(Writer writer) throws IOException {
		for (Variable v : variables()) {
			String text = String.format("%s %s;\n", v.scope().toKeyword(), v.name()); //$NON-NLS-1$
			writer.append(text);
		}
		writer.append("\n"); //$NON-NLS-1$
		for (Function f : functions()) {
			String returnType = f.returnType().toString();
			String desc = f.obtainUserDescription();
			if (desc != null) {
				if (desc.contains("\n")) { //$NON-NLS-1$
					desc = String.format("/*\n%s\n*/\n", desc); //$NON-NLS-1$
				} else {
					desc = String.format("//%s\n", desc); //$NON-NLS-1$
				}
			} else
				desc = ""; //$NON-NLS-1$
			String text = String.format("%s %s %s %s;\n", f.visibility().toKeyword(), Keywords.Func, returnType, f.getLongParameterString(true, true)); //$NON-NLS-1$
			writer.append(text);
		}
	}
	
	public void writeEngineScript() throws IOException {
		for (IStorageLocation loc : storageLocations) {
			URL scriptFile = loc.getURL(loc.name()+".c", true);
			if (scriptFile != null) {
				OutputStream output = loc.outputStreamForURL(scriptFile);
				if (output != null) try {
					Writer writer = new OutputStreamWriter(output);
					try {
						writeEngineScript(writer);
					} finally {
						writer.close();
					}
				} finally {
					output.close();
				}
				break;
			}
		}
	}
	
	public void saveSettings() {
		if (!hasCustomSettings())
			return;
		for (IStorageLocation loc : storageLocations) {
			URL settingsFile = loc.getURL(CONFIGURATION_INI_NAME, true);
			if (settingsFile != null) {
				OutputStream output = loc.outputStreamForURL(settingsFile);
				if (output != null) {
					try {
						currentSettings.saveTo(output, intrinsicSettings);
					} finally {
						try {
							output.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					break;
				}
			}
		}
	}
	
	public Collection<URL> getURLsOfStorageLocationPath(String configurationFolder, boolean onlyFromReadonlyStorageLocation) {
		LinkedList<URL> result = new LinkedList<URL>();
		for (IStorageLocation loc : storageLocations) {
			if (onlyFromReadonlyStorageLocation && loc.toFolder() != null)
				continue;
			loc.getURLsOfContainer(configurationFolder, true, result);
		}
		return result;
	}
	
	public OutputStream outputStreamForStorageLocationEntry(String entryPath) {
		for (IStorageLocation loc : storageLocations) {
			URL url = loc.getURL(entryPath, true);
			if (url != null) {
				OutputStream result = loc.outputStreamForURL(url);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}
	
	@Override
	public void clearDeclarations() {
		super.clearDeclarations();
		cachedPrefixedVariables = null;
	}
	
	public Variable[] variablesWithPrefix(String prefix) {
		// FIXME: oh noes, will return array stored in map, making it possible to modify it
		if (cachedPrefixedVariables != null) {
			Variable[] inCache = cachedPrefixedVariables.get(prefix);
			if (inCache != null)
				return inCache;
		}
		List<Variable> result = new LinkedList<Variable>();
		for (Variable v : variables()) {
			if (v.scope() == Scope.CONST && v.name().startsWith(prefix)) {
				result.add(v);
			}
		}
		Variable[] resultArray = result.toArray(new Variable[result.size()]);
		if (cachedPrefixedVariables == null)
			cachedPrefixedVariables = new HashMap<String, Variable[]>();
		cachedPrefixedVariables.put(prefix, resultArray);
		return resultArray;
	}
	
	public Process executeEmbeddedUtility(String name, String... args) {
		if (!currentSettings().supportsEmbeddedUtilities)
			return null;
		String path = currentSettings().engineExecutablePath;
		if (path != null) {
			String[] completeArgs = new String[2+args.length];
			completeArgs[0] = path;
			completeArgs[1] = "-x"+name;
			for (int i = 0; i < args.length; i++)
				completeArgs[2+i] = args[i];
			try {
				final Process p = Runtime.getRuntime().exec(completeArgs);
				new Thread() {
					@Override
					public void run() {
						try {
							IOConsoleOutputStream stream = Utilities.clonkConsole().newOutputStream();
							byte[] buffer = new byte[1024];
							int bytesRead;
							while ((bytesRead = p.getInputStream().read(buffer)) > 0)
								stream.write(buffer, 0, bytesRead);
						} catch (IOException e) {
							e.printStackTrace();
						}
					};
				}.start();
				return p;
			} catch (IOException e) {
				System.out.println("Failed to execute utility " + name);
				return null;
			}
		} else
			return null;
	}
	
	public C4Group.GroupType groupTypeForExtension(String ext) {
		C4Group.GroupType gt = currentSettings.fileExtensionToGroupTypeMapping().get(ext);
		if (gt != null)
			return gt;
		else
			return C4Group.GroupType.OtherGroup;
	}
	
	public C4Group.GroupType groupTypeForFileName(String fileName) {
		return groupTypeForExtension(fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase()); //$NON-NLS-1$
	}
	
	public Object image(String name, boolean returnDescriptor) {
		Collection<URL> urls = getURLsOfStorageLocationPath("images", false);
		if (urls != null)
			for (URL url : urls)
				if (url.getFile().endsWith(name+".png"))
					return returnDescriptor ? UI.imageDescriptorForURL(url) :  UI.imageForURL(url);
		return null;
	}
	
	public Image image(GroupType groupType) {return (Image) image(groupType.name(), false);}
	public ImageDescriptor imageDescriptor(GroupType groupType) {return (ImageDescriptor) image(groupType.name(), true);}
	public Image image(String name) {return (Image) image(name, false);}
	public ImageDescriptor imageDescriptor(String name) {return (ImageDescriptor) image(name, true);}
	
	/**
	 * Construct group name based on the name without extension and a {@link GroupType}
	 * @param name The name without extension
	 * @param groupType The group type
	 * @return Group name with correct extension.
	 */
	public String groupName(String name, GroupType groupType) {
		return name + "." + currentSettings().groupTypeToFileExtensionMapping().get(groupType);
	}
	
	private final XMLDocImporter xmlDocImporter = new XMLDocImporter();
	private IniDescriptionsLoader iniDescriptionsLoader;
	
	/**
	 * Return a XML Documentation importer for importing documentation from the repository path specified in the {@link #currentSettings()}.
	 * @return
	 */
	public XMLDocImporter repositoryDocImporter() {
		synchronized (xmlDocImporter) {
			xmlDocImporter.setRepositoryPath(currentSettings().repositoryPath);
			return xmlDocImporter.initialize();
		}
	}

	public IStorageLocation[] storageLocations() {
		return storageLocations;
	}
	
	@Override
	public String qualifiedName() {
		return name();
	}

}
