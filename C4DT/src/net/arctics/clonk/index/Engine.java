package net.arctics.clonk.index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.Problem;
import net.arctics.clonk.parser.c4script.BuiltInDefinitions;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.CPPSourceDeclarationsImporter;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Function.ParameterStringOption;
import net.arctics.clonk.parser.c4script.IHasName;
import net.arctics.clonk.parser.c4script.ITypeable;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.SpecialEngineRules;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.c4script.XMLDocImporter;
import net.arctics.clonk.parser.c4script.XMLDocImporter.ExtractedDeclarationDocumentation;
import net.arctics.clonk.parser.inireader.CustomIniUnit;
import net.arctics.clonk.parser.inireader.IniData;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
import net.arctics.clonk.util.IHasUserDescription;
import net.arctics.clonk.util.IStorageLocation;
import net.arctics.clonk.util.LineNumberObtainer;
import net.arctics.clonk.util.SettingsBase;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
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
	private static final String CONFIGURATION_INI_NAME = "configuration.ini"; //$NON-NLS-1$

	private CachedEngineDeclarations cachedDeclarations;
	private Map<String, Variable[]> cachedPrefixedVariables;
	private EngineSettings intrinsicSettings;
	private EngineSettings currentSettings;
	private IStorageLocation[] storageLocations;
	private IniData iniConfigurations;
	private SpecialEngineRules specialRules;
	private Index index;
	private Scenario templateScenario;
	private final transient XMLDocImporter xmlDocImporter = new XMLDocImporter();
	private IniDescriptionsLoader iniDescriptionsLoader;

	/**
	 * Return the {@link SpecialEngineRules} object associated with this engine. It is an instance of specialEngineRules_&lt;name&gt;
	 * @return The {@link SpecialEngineRules} object
	 */
	public SpecialEngineRules specialRules() { return specialRules; }

	/**
	 * Return the {@link EngineSettings} read from the configuration.ini file embedded into the plugin jar.
	 * @return The intrinsic settings
	 */
	public EngineSettings intrinsicSettings() { return intrinsicSettings; }

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
			!Utilities.eq(oldSettings.repositoryPath, settings.repositoryPath) ||
			oldSettings.readDocumentationFromRepository != settings.readDocumentationFromRepository
		)
			if (settings.readDocumentationFromRepository)
				reinitializeDocImporter();
			else
				parseEngineScript();
	}

	/**
	 * Return the currently active {@link EngineSettings}.
	 * @return The current settings
	 */
	public EngineSettings settings() { return currentSettings; }

	/**
	 * Return {@link CachedEngineDeclarations} for this engine.
	 * @return The {@link CachedEngineDeclarations}
	 */
	public final CachedEngineDeclarations cachedDeclarations() { return cachedDeclarations; }

	/**
	 * Return {@link IniData} configuration for this engine
	 * @return The {@link IniData} configuration
	 */
	public IniData iniConfigurations() { return iniConfigurations; }

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
		cachedDeclarations = new CachedEngineDeclarations(this);
		cachedPrefixedVariables = null;
	}

	/**
	 * Return an array containing possible engine executable paths according to the OS Eclipse runs on.
	 * The paths are relative to a Clonk folder.
	 * @return The array of executable paths
	 */
	public static String[] possibleEngineNamesAccordingToOS() {
		// reordered to save lots of cpu time
		if (Util.isWindows())
			return new String[] { "Clonk.c4x", "Clonk.exe" }; //$NON-NLS-1$ //$NON-NLS-2$
		if (Util.isLinux())
			return new String[] { "clonk" }; //$NON-NLS-1$
		if (Util.isMac())
			return new String[] {
				"clonk.app/Contents/MacOS/clonk", //$NON-NLS-1$
				"Clonk.app/Contents/MacOS/Clonk" //$NON-NLS-1$
			};
		// assume some UNIX -.-
		return new String[] { "clonk" }; //$NON-NLS-1$
	}

	/**
	 * Return whether this engine accepts the given text as ID
	 * @param text The ID text
	 * @return Whether accepted or not
	 */
	public boolean acceptsId(String text) { return specialRules().parseId(new BufferedScanner(text)) != null; }
	private boolean hasCustomSettings() { return !currentSettings.equals(intrinsicSettings); }

	/**
	 * I am my own engine!
	 */
	@Override
	public Engine engine() { return this; }
	@Override
	public IFile source() { return null; }
	public Scenario templateScenario() { return templateScenario; }

	@Override
	public void postLoad(Declaration parent, Index root) {
		super.postLoad(parent, root);
		resetCache();
	}

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
			URL settingsFile = loc.locatorForEntry(CONFIGURATION_INI_NAME, false);
			if (settingsFile != null) {
				InputStream input = settingsFile.openStream();
				try {
					if (currentSettings == null) {
						intrinsicSettings = SettingsBase.createFrom(EngineSettings.class, input);
						currentSettings = (EngineSettings) intrinsicSettings.clone();
					} else
						currentSettings.loadFrom(input);
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
				URL confFile = loc.locatorForEntry("iniconfig.xml", false); //$NON-NLS-1$
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
			if (findFunction("this") == null) //$NON-NLS-1$
				this.addDeclaration(new Function("this", Function.FunctionScope.GLOBAL)); //$NON-NLS-1$
			if (findVariable(Keywords.Nil) == null)
				this.addDeclaration(new Variable(Keywords.Nil, Variable.Scope.CONST));
		} finally {
			resetCache();
		}
	}

	private transient Thread documentationPrefetcherThread;
	private void createDeclarationsFromRepositoryDocumentationFiles() {
		File[] xmlFiles = new File(settings().repositoryPath+"/docs/sdk/script/fn").listFiles(); //$NON-NLS-1$
		final List<IDocumentedDeclaration> createdDecs = new ArrayList<IDocumentedDeclaration>(xmlFiles.length);
		for (File xmlFile : xmlFiles) {
			boolean isConst = false;
			try {
				FileReader r = new FileReader(xmlFile);
				try {
					for (String l : StringUtil.lines(r))
						if (l.contains("<const>")) { //$NON-NLS-1$
							isConst = true;
							break;
						} else if (l.contains("<func>")) { //$NON-NLS-1$
							isConst = false;
							break;
						}
				} finally {
					r.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			String rawFileName = StringUtil.rawFileName(xmlFile.getName());
			if (BuiltInDefinitions.KEYWORDS.contains(rawFileName))
				continue;
			IDocumentedDeclaration dec;
			if (isConst)
				dec = new DocumentedVariable(rawFileName, Variable.Scope.CONST);
			else
				dec = new DocumentedFunction(rawFileName, Function.FunctionScope.GLOBAL);
			this.addDeclaration((Declaration)dec);
			createdDecs.add(dec);
		}
		prefetchDocumentation(createdDecs);
	}

	private void prefetchDocumentation(final List<IDocumentedDeclaration> createdDecs) {
		documentationPrefetcherThread = new Thread() {
			@Override
			public void run() {
				try {
					for (IDocumentedDeclaration dec : createdDecs) {
						if (this != documentationPrefetcherThread || Core.stopped())
							break;
						dec.fetchDocumentation();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				synchronized (Engine.this) {
					if (this == documentationPrefetcherThread)
						documentationPrefetcherThread = null;
				}
			}
		};
		documentationPrefetcherThread.setPriority(Thread.MIN_PRIORITY);
		documentationPrefetcherThread.start();
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
			populateDictionary();
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
			URL url = loc.locatorForEntry("declarations.ini", false); //$NON-NLS-1$
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
					try {
						unit.parser().parse(false);
					} catch (ParsingException e) {
						e.printStackTrace();
					}
					for (IniSection section : unit.sections()) {
						Declaration declaration = findDeclaration(section.name());
						if (declaration != null)
							section.commit(declaration, false);
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
		final String lang = ClonkPreferences.languagePref();
		for (IStorageLocation loc : storageLocations)
			for (String s : new String[] {String.format("%s%s.c", name(), lang), String.format("%s.c", name())}) {
				final URL url = loc.locatorForEntry(s, false); //$NON-NLS-1$
				if (url != null)
					try (InputStream stream = url.openStream()) {
						String scriptFromStream = StreamUtil.stringFromInputStream(stream);
						final LineNumberObtainer lno = new LineNumberObtainer(scriptFromStream);
						C4ScriptParser parser = new C4ScriptParser(scriptFromStream, this, null) {
							private boolean firstMessage = true;
							@Override
							public void marker(Problem code,
								int errorStart, int errorEnd, int flags,
								int severity, Object... args) throws ParsingException {
								if (firstMessage) {
									firstMessage = false;
									System.out.println("Messages while parsing " + url.toString()); //$NON-NLS-1$
								}
								System.out.println(String.format(
									"%s @(%d, %d)", //$NON-NLS-1$
									code.makeErrorString(args),
									lno.obtainLineNumber(errorStart),
									lno.obtainCharNumberInObtainedLine()
								));
								super.marker(code, errorStart, errorEnd, flags, severity, args);
							}
							@Override
							protected Function newFunction(String nameWillBe) { return new EngineFunction(); }
							@Override
							public Variable newVariable(String varName, Scope scope) { return new EngineVariable(varName, scope); }
						};
						try {
							parser.parse();
						} catch (ParsingException e) {
							e.printStackTrace();
						}
						postLoad((Declaration)null, (Index)null);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
			}
	}

	private void createSpecialRules() {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends SpecialEngineRules> rulesClass = (Class<? extends SpecialEngineRules>) Engine.class.getClassLoader().loadClass(
				String.format("%s.SpecialEngineRules_%s", SpecialEngineRules.class.getPackage().getName(), name())); //$NON-NLS-1$
			specialRules = rulesClass.newInstance();
			specialRules.initialize();
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
				URL url = location.locatorForEntry(CONFIGURATION_INI_NAME, false);
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

	public static class TemplateScenario extends Scenario {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		public static class Ticket implements Serializable, ISerializationResolvable {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			@Override
			public Object resolve(Index index) {
				return index.engine().templateScenario();
			}
		}
		public TemplateScenario(Index index, String name, IContainer container) { super(index, name, container); }
		@Override
		public Object saveReplacement(Index context) { return new Ticket(); }
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
		try {
			loadProjectConversionConfigurations();
		} catch (Exception e) {
			e.printStackTrace();
		}
		reinitializeDocImporter();

		index = new Index() {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			@Override
			public Engine engine() {
				return Engine.this;
			}
		};
		templateScenario = new TemplateScenario(index, "ScenarioOfTheMind", null);
	}

	private final Map<String, ProjectConversionConfiguration> projectConversionConfigurations = new HashMap<String, ProjectConversionConfiguration>();

	private void loadProjectConversionConfigurations() {
		projectConversionConfigurations.clear();
		for (int i = storageLocations.length-1; i >= 0; i--) {
			IStorageLocation location = storageLocations[i];
			List<URL> projectConverterFiles = new ArrayList<URL>();
			location.collectURLsOfContainer("projectConverters", true, projectConverterFiles); //$NON-NLS-1$
			if (projectConverterFiles.size() > 0) {
				Map<String, List<URL>> buckets = new HashMap<String, List<URL>>();
				for (URL url : projectConverterFiles) {
					Path path = new Path(url.getPath());
					String engineName = path.segment(path.segmentCount()-2);
					if (engineName.equals("projectConverters")) //$NON-NLS-1$
						continue; // bogus file next to engine-specific folders
					List<URL> bucket = buckets.get(engineName);
					if (bucket == null) {
						bucket = new ArrayList<URL>();
						buckets.put(engineName, bucket);
					}
					bucket.add(url);
				}
				for (Map.Entry<String, List<URL>> bucket : buckets.entrySet()) {
					ProjectConversionConfiguration conf = new ProjectConversionConfiguration(this);
					conf.load(bucket.getValue());
					projectConversionConfigurations.put(bucket.getKey(), conf);
				}
			}
		}
	}

	public ProjectConversionConfiguration projectConversionConfigurationForEngine(Engine engine) {
		loadProjectConversionConfigurations();
		return projectConversionConfigurations.get(engine.name());
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
				if (desc.contains("\n")) //$NON-NLS-1$
					desc = String.format("/*\n%s\n*/\n", desc); //$NON-NLS-1$
				else
					desc = String.format("//%s\n", desc); //$NON-NLS-1$
				writer.append(desc);
			}
			String text = String.format("%s %s %s %s;\n", f.visibility().toKeyword(), Keywords.Func, returnType, //$NON-NLS-1$
				f.parameterString(EnumSet.of(
					ParameterStringOption.FunctionName,
					ParameterStringOption.EngineCompatible,
					ParameterStringOption.ParameterComments
				)));
			writer.append(text);
		}
	}

	public void writeEngineScript() throws IOException {
		for (IStorageLocation loc : storageLocations) {
			URL scriptFile = loc.locatorForEntry(loc.name()+".c", true); //$NON-NLS-1$
			if (scriptFile != null) {
				OutputStream output = loc.outputStreamForURL(scriptFile);
				if (output != null) try (Writer writer = new OutputStreamWriter(output)) {
					writeEngineScript(writer);
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
			URL settingsFile = loc.locatorForEntry(CONFIGURATION_INI_NAME, true);
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
			loc.collectURLsOfContainer(configurationFolder, true, result);
		}
		return result;
	}

	public OutputStream outputStreamForStorageLocationEntry(String entryPath) {
		for (IStorageLocation loc : storageLocations) {
			URL url = loc.locatorForEntry(entryPath, true);
			if (url != null) {
				OutputStream result = loc.outputStreamForURL(url);
				if (result != null)
					return result;
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
		for (Variable v : variables())
			if (v.scope() == Scope.CONST && v.name().startsWith(prefix))
				result.add(v);
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
			completeArgs[1] = "-x"+name; //$NON-NLS-1$
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
				System.out.println("Failed to execute utility " + name); //$NON-NLS-1$
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
		Collection<URL> urls = getURLsOfStorageLocationPath("images", false); //$NON-NLS-1$
		if (urls != null)
			for (URL url : urls)
				if (url.getFile().endsWith(name+".png")) //$NON-NLS-1$
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
		return name + "." + settings().groupTypeToFileExtensionMapping().get(groupType); //$NON-NLS-1$
	}

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

	public IStorageLocation[] storageLocations() { return storageLocations; }
	@Override
	public String qualifiedName() { return name(); }

	public boolean supportsPrimitiveType(PrimitiveType type) {
		switch (type) {
		case NUM:
			return settings().supportsFloats;
		case REFERENCE:
			return settings().supportsRefs;
		case PROPLIST:
			return settings().supportsProplists;
		default:
			return true;
		}
	}

}
