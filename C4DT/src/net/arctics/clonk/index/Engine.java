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

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.CPPSourceDeclarationsImporter;
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
 * Container for engine functions and constants. Will be either initialized by parsing a special 'engine script' embedded into the plugin jar which
 * contains definitions for engine definitions in a slightly modified c4script syntax (return type specification, no function bodies), or
 * by parsing c++ source files from a source repository of the OpenClonk kind.
 * @author Madeen
 *
 */
public class Engine extends Script implements IndexEntity.TopLevelEntity {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	/**
	 * Settings object encapsulating configuration options for a specific engine.
	 * Read from res/engines/&lt;engine&gt;/configuration.ini and <workspace>/.metadata/.plugins/net.arctics.clonk/engines/&lt;engine&gt;/configuration.ini
	 * @author madeen
	 *
	 */
	public static class EngineSettings extends SettingsBase {

		private static final String SOURCE = "Source";
		private static final String INTRINSIC = "Intrinsic";

		/** Default strictness level applied to scripts with no explicit #strict line. */
		@IniField(category=INTRINSIC)
		public long strictDefaultLevel;
		/** Maximum string length of string constants. */
		@IniField(category=INTRINSIC)
		public long maxStringLen;
		/** Whether engine supports colon ID syntax (:Clonk, :Firestone). Enforcing this syntax was discussed and then dropped. */
		@IniField(category=INTRINSIC)
		public boolean colonIDSyntax;
		/**
		 * Whether declarations of static non-const variables are allowed to include an assignment. OC added support for this.
		 */
		@IniField(category=INTRINSIC)
		public boolean nonConstGlobalVarsAssignment;
		/**
		 * HACK: In OC, object definition constants (Clonk, Firestone) actually are parsed as referring to a {@link Variable} object each {@link Definition} maintains as its 'proxy variable'.<br/>
		 * This toggle activates/deactivates this behavior.
		 * */ 
		@IniField(category=INTRINSIC)
		public boolean definitionsHaveProxyVariables;
		/** Whether engine supports ref parameters (int & x). OpenClonk stopped supporting it. */
		@IniField(category=INTRINSIC)
		public boolean supportsRefs;
		/** Whether engine supports creating a debug connection and single-stepping through C4Script code */
		@IniField(category=INTRINSIC)
		public boolean supportsDebugging;
		/** Name of editor mode option (CR: console, OC: editor) */
		@IniField(category=INTRINSIC)
		public String editorCmdLineOption;
		/** Format for commandline options (ClonkRage: /%s vs OpenClonk: --%s) */
		@IniField(category=INTRINSIC)
		public String cmdLineOptionFormat;
		/** Format for commandline options that take an argument --%s=%s */
		@IniField(category=INTRINSIC)
		public String cmdLineOptionWithArgumentFormat;
		/** Whether engine supports -x <command> */
		@IniField(category=INTRINSIC)
		public boolean supportsEmbeddedUtilities;
		/** Whether engine parser allows obj-> ~DoSomething() */
		@IniField(category=INTRINSIC)
		public boolean spaceAllowedBetweenArrowAndTilde;
		/** String of the form c4d->DefinitionGroup,... specifying what file extension denote what group type. */
		@IniField(category=INTRINSIC)
		public String fileExtensionToGroupTypeMapping;
		/** Extension for material definition files */
		@IniField(category=INTRINSIC)
		public String materialExtension;
		/** Engine supports proplists (OC) */
		@IniField(category=INTRINSIC)
		public boolean proplistsSupported;
		/** ';'-separated list of file extensions supported as sound files */
		@IniField(category=INTRINSIC)
		public String supportedSoundFileExtensions;
		/** Engine supports passing references to functions */
		@IniField(category=INTRINSIC)
		public boolean supportsFunctionRefs;
		@IniField(category=INTRINSIC)
		public boolean supportsFloats;
		@IniField(category=INTRINSIC)
		public boolean supportsGlobalProplists;
		
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
		
		// Fields related to scanning cpp source files for built-in declarations. 
		
		@IniField(category=SOURCE)
		public String initFunctionMapPattern;
		@IniField(category=SOURCE)
		public String constMapPattern;
		@IniField(category=SOURCE)
		public String fnMapPattern;
		@IniField(category=SOURCE)
		public String fnMapEntryPattern;
		@IniField(category=SOURCE)
		public String constMapEntryPattern;
		@IniField(category=SOURCE)
		public String addFuncPattern;
		@IniField(category=SOURCE)
		public String fnDeclarationPattern;
		/**
		 * List of cpp source file paths - relative to a repository - to import declarations from. Used by {@link CPPSourceDeclarationsImporter}
		 */
		@IniField(category=SOURCE)
		public String cppSources;
		/**
		 * Functions defined by scripts, called by engine
		 */
		@IniField(category=SOURCE)
		public String callbackFunctions;
		
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
		
		/**
		 * Reverse map of {@link #fileExtensionToGroupTypeMapping()}
		 * @return The reverse map
		 */
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

		/**
		 * Return documentation URL for a function name. The result will be a local file URL if {@link #useDocsFromRepository} is set,
		 * a link based on the {@link #docURLTemplate} if not.
		 * @param functionName The funciton name
		 * @return The URL string
		 */
		public String documentationURLForFunction(String functionName) {
			String urlFormatString = useDocsFromRepository
				? "file://" + repositoryPath + "/docs/sdk/script/fn/%1$s.xml"
				: docURLTemplate;
			return String.format(urlFormatString, functionName, ClonkPreferences.getLanguagePrefForDocumentation());
		}
		
		/**
		 * Return a list of sound file extensions this engine supports.
		 * @return The extension list
		 */
		public List<String> supportedSoundFileExtensions() {
			if (supportedSoundFileExtensions_ == null) {
				supportedSoundFileExtensions_ = Arrays.asList(supportedSoundFileExtensions.split("\\;"));
			}
			return supportedSoundFileExtensions_;
		}
		
		public String[] callbackFunctions() {
			return callbackFunctions != null ? callbackFunctions.split(",") : new String[0];
		}

	}

	private transient CachedEngineDeclarations cachedFuncs;
	private transient Map<String, Variable[]> cachedPrefixedVariables;

	private transient EngineSettings intrinsicSettings;
	private transient EngineSettings currentSettings;
	
	private transient IStorageLocation[] storageLocations;
	private transient IniData iniConfigurations;
	
	private transient SpecialScriptRules specialScriptRules;
	
	/**
	 * Return the {@link SpecialScriptRules} object associated with this engine. It is an instance of SpecialScriptRules_&lt;name&gt;
	 * @return The {@link SpecialScriptRules} object
	 */
	public SpecialScriptRules specialScriptRules() {
		return specialScriptRules;
	}

	/**
	 * Return the {@link EngineSettings} read from the configuration.ini file embedded into the plugin jar.
	 * @return The intrinsic settings
	 */
	public EngineSettings intrinsicSettings() {
		return intrinsicSettings;
	}

	/**
	 * Set {@link #settings()}, save them to the workspace configuration.ini file and reinitialize anything that needs reinitializing
	 * based on modification of settings such as {@link EngineSettings#readDocumentationFromRepository}
	 * @param settings The settings to apply
	 */
	public void applySettings(EngineSettings settings) {
		EngineSettings oldSettings = this.currentSettings;
		this.currentSettings = settings;
		saveSettings();
		if (
			!Utilities.objectsEqual(oldSettings.repositoryPath, settings.repositoryPath) ||
			oldSettings.readDocumentationFromRepository != settings.readDocumentationFromRepository
		) {
			if (settings.readDocumentationFromRepository)
				reinitializeDocImporter();
			else
				parseEngineScript();
		}
	}

	/**
	 * Return the currently active {@link EngineSettings}.
	 * @return The current settings
	 */
	public EngineSettings settings() {
		return currentSettings;
	}

	/**
	 * Return {@link CachedEngineDeclarations} for this engine.
	 * @return The {@link CachedEngineDeclarations}
	 */
	public final CachedEngineDeclarations cachedFuncs() {
		return cachedFuncs;
	}
	
	/**
	 * Return {@link IniData} configuration for this engine
	 * @return The {@link IniData} configuration
	 */
	public IniData iniConfigurations() {
		return iniConfigurations;
	}

	/**
	 * Create new engine with the specified name.
	 * @param name The name of the engine
	 */
	public Engine(String name) {
		super(null);
		setName(name);
		intrinsicSettings = new EngineSettings();
		resetCache();
	}

	private void resetCache() {
		cachedFuncs = new CachedEngineDeclarations(this);
		cachedPrefixedVariables = null;
	}

	/**
	 * Return an array containing possible engine executable paths according to the OS Eclipse runs on.
	 * The paths are relative to a Clonk folder.
	 * @return The array of executable paths
	 */
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

	/**
	 * Return whether this engine accepts the given text as ID
	 * @param text The ID text
	 * @return Whether accepted or not
	 */
	public boolean acceptsId(String text) {
		return specialScriptRules().parseId(new BufferedScanner(text)) != null;
	}

	private boolean hasCustomSettings() {
		return !currentSettings.equals(intrinsicSettings);
	}

	/**
	 * I am my own engine!
	 */
	@Override
	public Engine engine() {
		return this;
	}

	@Override
	public IFile scriptStorage() {
		return null;
	}
	
	@Override
	public void postLoad(Declaration parent, Index root) {
		super.postLoad(parent, root);
		resetCache();
	}
	
	private static final String CONFIGURATION_INI_NAME = "configuration.ini"; //$NON-NLS-1$
	
	/**
	 * Load settings from this engine's {@link #storageLocations()}
	 * @throws IOException
	 */
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
	
	/**
	 * Load ini configuration from this engine's {@link #storageLocations()}
	 */
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
			CPPSourceDeclarationsImporter importer = new CPPSourceDeclarationsImporter();
			importer.overwriteExistingDeclarations = false;
			importer.importFromRepository(this, settings().repositoryPath, new NullProgressMonitor());
			if (findFunction("this") == null)
				this.addDeclaration(new Function("this", Function.FunctionScope.GLOBAL));
			if (findVariable(Keywords.Nil) == null)
				this.addDeclaration(new Variable(Keywords.Nil, Variable.Scope.CONST));
		} finally {
			resetCache();
		}
	}

	private void createDeclarationsFromRepositoryDocumentationFiles() {
		for (File xmlFile : new File(settings().repositoryPath+"/docs/sdk/script/fn").listFiles()) {
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
	 * the next time documentation needs to be obtained via {@link #obtainDescription(IHasUserDescription)},
	 * a repository doc reading will be performed.
	 */
	public void reinitializeDocImporter() {
		xmlDocImporter.discardInitialization();
		if (settings().readDocumentationFromRepository) {
			xmlDocImporter.setRepositoryPath(settings().repositoryPath);
			namesOfDeclarationsForWhichDocsWereFreshlyObtained.clear();
			xmlDocImporter.initialize();
			createPlaceholderDeclarationsToBeFleshedOutFromDocumentation();
		}
	}
	
	/**
	 * Obtain a description for the specified declaration. From which source this description is lifted depends on settings
	 * such as {@link EngineSettings#readDocumentationFromRepository}. If that setting is set attempts will be made to extract
	 * the description from the <code>repository</code>/docs folder, with the fallback being descriptions<code>lang</code>.ini files in the res/engines/<code>engine</code> folder.
	 * @param declaration The declaration for which to obtain a description
	 * @return The description or null if I don't know what went wrong
	 */
	public <T extends IHasUserDescription & IHasName> String obtainDescription(T declaration) {
		if (declaration.userDescription() != null && namesOfDeclarationsForWhichDocsWereFreshlyObtained.contains(declaration.name()))
			return declaration.userDescription();
		applyDocumentationAndSignatureFromRepository(declaration);
		return declaration.userDescription();
	}

	private <T extends IHasUserDescription & IHasName> boolean applyDocumentationAndSignatureFromRepository(T declaration) {
		namesOfDeclarationsForWhichDocsWereFreshlyObtained.add(declaration.name());
		// dynamically load from repository
		if (settings().readDocumentationFromRepository) {
			XMLDocImporter importer = repositoryDocImporter().initialize();
			ExtractedDeclarationDocumentation d = importer.extractDeclarationInformationFromFunctionXml(declaration.name(), ClonkPreferences.languagePref(), XMLDocImporter.DOCUMENTATION);
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
	
	private void loadDeclarationsConfiguration() {
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
	
	private void parseEngineScript() {
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
				String.format("%s.parser.c4script.specialscriptrules.SpecialScriptRules_%s", Core.PLUGIN_ID, name()));
			specialScriptRules = rulesClass.newInstance();
			specialScriptRules.initialize();
		} catch (ClassNotFoundException e) {
			// ignore
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Load an {@link Engine} from a list of {@link IStorageLocation}s
	 * @param locations The locations, which usually are just two, one representing a location in the user's workspace/.metadata folder and the other being embedded in the plugin jar.
	 * @return The loaded {@link Engine}
	 */
	public static Engine loadFromStorageLocations(final IStorageLocation... locations) {
		Engine result = null;
		try {
			for (IStorageLocation location : locations) {
				if (location == null)
					continue;
				// only consider valid engine folder if configuration.ini is present
				URL url = location.getURL(CONFIGURATION_INI_NAME, false); 
				if (url != null) {
					result = new Engine(location.name());
					result.load(locations);
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
		if (!settings().readDocumentationFromRepository)
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
			String text = String.format("%s %s %s %s;\n", f.visibility().toKeyword(), Keywords.Func, returnType, f.longParameterString(true, true)); //$NON-NLS-1$
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
	
	/**
	 * Save the settings!
	 */
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
		if (!settings().supportsEmbeddedUtilities)
			return null;
		String path = settings().engineExecutablePath;
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
		return name + "." + settings().groupTypeToFileExtensionMapping().get(groupType);
	}
	
	private final XMLDocImporter xmlDocImporter = new XMLDocImporter();
	private IniDescriptionsLoader iniDescriptionsLoader;
	
	/**
	 * Return a XML Documentation importer for importing documentation from the repository path specified in the {@link #settings()}.
	 * @return
	 */
	public XMLDocImporter repositoryDocImporter() {
		synchronized (xmlDocImporter) {
			xmlDocImporter.setRepositoryPath(settings().repositoryPath);
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
