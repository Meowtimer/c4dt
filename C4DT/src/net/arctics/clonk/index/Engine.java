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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4group.C4Group;
import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.c4script.BuiltInDefinitions;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Function.PrintParametersOptions;
import net.arctics.clonk.c4script.IHasName;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.SpecialEngineRules;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.Variable.Scope;
import net.arctics.clonk.c4script.typing.ITypeable;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.Typing;
import net.arctics.clonk.index.XMLDocImporter.ExtractedDeclarationDocumentation;
import net.arctics.clonk.ini.CustomIniUnit;
import net.arctics.clonk.ini.IniData;
import net.arctics.clonk.ini.IniData.IniConfiguration;
import net.arctics.clonk.ini.IniSection;
import net.arctics.clonk.ini.IniUnitParser;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.Console;
import net.arctics.clonk.util.IHasUserDescription;
import net.arctics.clonk.util.IStorageLocation;
import net.arctics.clonk.util.SettingsBase;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
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
	private static final String CONFIGURATION_INI_NAME = "configuration.ini"; //$NON-NLS-1$

	private CachedEngineDeclarations cachedDeclarations;
	private Map<String, Variable[]> cachedPrefixedVariables;
	private EngineSettings intrinsicSettings;
	private EngineSettings settings;
	private IStorageLocation[] storageLocations;
	private IniData iniConfigurations;
	private SpecialEngineRules specialRules;
	private Scenario templateScenario;
	private final transient XMLDocImporter xmlDocImporter = new XMLDocImporter();

	public XMLDocImporter docImporter() { return xmlDocImporter; }

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
	public void applySettings(final EngineSettings settings) {
		final EngineSettings oldSettings = this.settings;
		this.settings = settings;
		saveSettings();
		if (
			!Utilities.eq(oldSettings.repositoryPath, settings.repositoryPath) ||
			oldSettings.readDocumentationFromRepository != settings.readDocumentationFromRepository
		)
			loadDeclarations();
	}

	public void loadDeclarations() {
		if (settings.readDocumentationFromRepository)
			reinitializeDocImporter();
		else
			parseEngineScript();
	}

	/**
	 * Return the currently active {@link EngineSettings}.
	 * @return The current settings
	 */
	public EngineSettings settings() { return settings; }

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
	public Engine(final String name) {
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
	public boolean acceptsId(final String text) { return specialRules().parseId(new BufferedScanner(text)) != null; }
	private boolean hasCustomSettings() { return !settings.equals(intrinsicSettings); }

	/**
	 * I am my own engine!
	 */
	@Override
	public Engine engine() { return this; }
	@Override
	public IFile source() { return null; }
	public Scenario templateScenario() { return templateScenario; }

	@Override
	public void postLoad(final Declaration parent, final Index root) {
		super.postLoad(parent, root);
		final Function f = findLocalFunction("this", false);
		if (f != null)
			removeDeclaration(f);
		resetCache();
	}

	/**
	 * Load settings from this engine's {@link #storageLocations()}
	 * @throws IOException
	 */
	public void loadSettings() throws IOException {
		// combine settings files in reverse-order so custom config is based on default
		for (int i = storageLocations.length-1; i >= 0; i--) {
			final IStorageLocation loc = storageLocations[i];
			if (loc == null)
				continue;
			final URL settingsFile = loc.locatorForEntry(CONFIGURATION_INI_NAME, false);
			if (settingsFile != null) {
				final InputStream input = settingsFile.openStream();
				try {
					if (settings == null) {
						intrinsicSettings = SettingsBase.createFrom(EngineSettings.class, input);
						settings = (EngineSettings) intrinsicSettings.clone();
					} else
						settings.loadFrom(input);
				} finally {
					input.close();
				}
			}
		}
		if (intrinsicSettings == null) {
			intrinsicSettings = new EngineSettings();
			settings = (EngineSettings) intrinsicSettings.clone();
		}
	}

	/**
	 * Load ini configuration from this engine's {@link #storageLocations()}
	 */
	private void loadIniConfigurations() {
		try {
			for (int i = storageLocations.length-1; i >= 0; i--) {
				final IStorageLocation loc = storageLocations[i];
				if (loc == null)
					continue;
				final URL confFile = loc.locatorForEntry("iniconfig.xml", false); //$NON-NLS-1$
				if (confFile != null) {
					final InputStream input = confFile.openStream();
					try {
						final IniData data = new IniData(input);
						data.parse();
						iniConfigurations = data; // set this variable when parsing completed successfully
					} finally {
						input.close();
					}
				}
			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private class DeclarationsConfiguration extends IniConfiguration {
		@Override
		public boolean hasSection(final String sectionName) {
			return Engine.this.findDeclaration(sectionName) != null;
		}
	}

	private final Set<String> namesOfDeclarationsForWhichDocsWereFreshlyObtained = new HashSet<String>();

	private void createPlaceholderDeclarationsToBeFleshedOutFromDocumentation() {
		this.clearDeclarations();
		try {
			createDeclarationsFromRepositoryDocumentationFiles();
			final CPPSourceDeclarationsImporter importer = new CPPSourceDeclarationsImporter();
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
		final File[] xmlFiles = new File(settings().repositoryPath+"/docs/sdk/script/fn").listFiles(); //$NON-NLS-1$
		final List<IDocumentedDeclaration> createdDecs = new ArrayList<IDocumentedDeclaration>(xmlFiles.length);
		for (final File xmlFile : xmlFiles) {
			boolean isConst = false;
			try {
				final FileReader r = new FileReader(xmlFile);
				try {
					for (final String l : StringUtil.lines(r))
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
			} catch (final Exception e) {
				e.printStackTrace();
				continue;
			}
			final String rawFileName = StringUtil.rawFileName(xmlFile.getName());
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
					for (final IDocumentedDeclaration dec : createdDecs) {
						if (this != documentationPrefetcherThread || Core.stopped())
							break;
						dec.fetchDocumentation();
					}
				} catch (final Exception e) {
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
			deriveInformation();
		}
	}

	/**
	 * Obtain a description for the specified declaration. From which source this description is lifted depends on settings
	 * such as {@link EngineSettings#readDocumentationFromRepository}. If that setting is set attempts will be made to extract
	 * the description from the <code>repository</code>/docs folder, with the fallback being descriptions<code>lang</code>.ini files in the res/engines/<code>engine</code> folder.
	 * @param declaration The declaration for which to obtain a description
	 * @return The description or null if I don't know what went wrong
	 */
	public <T extends IHasUserDescription & IHasName> String obtainDescription(final T declaration) {
		if (declaration.userDescription() != null && namesOfDeclarationsForWhichDocsWereFreshlyObtained.contains(declaration.name()))
			return declaration.userDescription();
		applyDocumentationAndSignatureFromRepository(declaration);
		return declaration.userDescription();
	}

	private <T extends IHasUserDescription & IHasName> boolean applyDocumentationAndSignatureFromRepository(final T declaration) {
		namesOfDeclarationsForWhichDocsWereFreshlyObtained.add(declaration.name());
		// dynamically load from repository
		if (settings().readDocumentationFromRepository) {
			final XMLDocImporter importer = repositoryDocImporter().initialize();
			final ExtractedDeclarationDocumentation d = importer.extractDeclarationInformationFromFunctionXml(declaration.name(), ClonkPreferences.languagePref(), XMLDocImporter.DOCUMENTATION);
			if (d != null) {
				declaration.setUserDescription(d.description);
				if (declaration instanceof Function) {
					final Function f = (Function)declaration;
					if (d.parameters != null)
						f.setParameters(d.parameters);
				}
				if (d.returnType != null && declaration instanceof ITypeable)
					((ITypeable)declaration).forceType(d.returnType);
				return true;
			}
		}
		return false;
	}

	private void loadDeclarationsConfiguration() {
		for (int i = storageLocations.length-1; i >= 0; i--) {
			final IStorageLocation loc = storageLocations[i];
			if (loc == null)
				continue;
			final URL url = loc.locatorForEntry("declarations.ini", false); //$NON-NLS-1$
			if (url != null) {
				InputStream stream;
				try {
					stream = url.openStream();
				} catch (final IOException e) {
					e.printStackTrace();
					continue;
				}
				try {
					final CustomIniUnit unit = new CustomIniUnit(stream, new DeclarationsConfiguration());
					try {
						new IniUnitParser(unit).parse(false);
					} catch (final ProblemException e) {
						e.printStackTrace();
					}
					for (final IniSection section : unit.sections()) {
						final Declaration declaration = findDeclaration(section.name());
						if (declaration != null)
							section.commit(declaration, false);
					}
				} finally {
					try {
						stream.close();
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void parseEngineScript() {
		final String lang = ClonkPreferences.languagePref();
		for (final IStorageLocation loc : storageLocations) {
			final URL url = loc.locatorForEntry(String.format("%s.c", name()), false); //$NON-NLS-1$
			if (url != null) {
				try (InputStream stream = url.openStream()) {
					final String engineScript = StreamUtil.stringFromInputStream(stream);
					final ScriptParser parser = new EngineScriptParser(engineScript, this, null, url);
					try {
						parser.parse();
					} catch (final ProblemException e) {
						e.printStackTrace();
					}
					postLoad((Declaration)null, (Index)null);
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
				new IniDescriptionsLoader(this).loadDescriptions(lang);
				break;
			}
		}
	}

	private void createSpecialRules() {
		try {
			@SuppressWarnings("unchecked")
			final
			Class<? extends SpecialEngineRules> rulesClass = (Class<? extends SpecialEngineRules>) Engine.class.getClassLoader().loadClass(
				String.format("%s.SpecialEngineRules_%s", SpecialEngineRules.class.getPackage().getName(), name())); //$NON-NLS-1$
			specialRules = rulesClass.getConstructor(Engine.class).newInstance(this);
			specialRules.initialize();
		} catch (final ClassNotFoundException e) {
			// ignore
		}
		catch (final Exception e) {
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
			for (final IStorageLocation location : locations) {
				if (location == null)
					continue;
				// only consider valid engine folder if configuration.ini is present
				final URL url = location.locatorForEntry(CONFIGURATION_INI_NAME, false);
				if (url != null) {
					result = new Engine(location.name());
					result.load(locations);
					break;
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static class TemplateScenario extends Scenario {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		public static class Ticket implements Serializable, IDeserializationResolvable {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			@Override
			public Object resolve(final Index index, final IndexEntity deserializee) {
				return index.engine().templateScenario();
			}
		}
		public TemplateScenario(final Index index, final String name, final IContainer container) { super(index, name, container); }
		@Override
		public Object saveReplacement(final Index context) { return new Ticket(); }
	}

	private void load(final IStorageLocation... providers) {
		this.storageLocations = providers;
		load();
	}

	public void load() {
		clearDeclarations();
		try {
			loadSettings();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		loadIniConfigurations();
		createSpecialRules();
		loadDeclarations();
		loadDeclarationsConfiguration();

		index = new Index() {
			private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			@Override
			public Engine engine() { return Engine.this; }
			@Override
			public Typing typing() { return Typing.STATIC; }
		};
		templateScenario = new TemplateScenario(index, "ScenarioOfTheMind", null);
		if (specialRules != null)
			specialRules.contribute(this);
	}

	public ProjectConversionConfiguration loadProjectConversionConfiguration(Engine sourceEngine) {
		try {
			final List<URL> projectConverterFiles = new ArrayList<URL>();
			for (int i = storageLocations.length - 1; i >= 0; i--) {
				final IStorageLocation location = storageLocations[i];
				location.collectURLsOfContainer(String.format("projectConverters/%s", sourceEngine.name()), true, projectConverterFiles); //$NON-NLS-1$
			}
			if (projectConverterFiles.size() > 0) {
				final ProjectConversionConfiguration conf = new ProjectConversionConfiguration(sourceEngine, this, projectConverterFiles);
				return conf;
			}
			return null;
		} catch (final Exception e) { e.printStackTrace(); return null; }
	}

	public void writeEngineScript(final Writer writer) throws IOException {
		for (final Variable v : variables()) {
			final String text = String.format("%s %s;\n", v.scope().toKeyword(), v.name()); //$NON-NLS-1$
			writer.append(text);
		}
		writer.append("\n"); //$NON-NLS-1$
		for (final Function f : functions()) {
			final String returnType = f.returnType().toString();
			String desc = f.obtainUserDescription();
			if (desc != null) {
				if (desc.contains("\n")) //$NON-NLS-1$
					desc = String.format("/*\n%s\n*/\n", desc); //$NON-NLS-1$
				else
					desc = String.format("//%s\n", desc); //$NON-NLS-1$
				writer.append(desc);
			}
			final String text = String.format("%s %s %s %s;\n", f.visibility().toKeyword(), Keywords.Func, returnType, //$NON-NLS-1$
				f.parameterString(new PrintParametersOptions(typings().get(f), true, true, true)));
			writer.append(text);
		}
	}

	public void writeEngineScript() throws IOException {
		for (final IStorageLocation loc : storageLocations) {
			final URL scriptFile = loc.locatorForEntry(loc.name()+".c", true); //$NON-NLS-1$
			if (scriptFile != null) {
				final OutputStream output = loc.outputStreamForURL(scriptFile);
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
		for (final IStorageLocation loc : storageLocations) {
			final URL settingsFile = loc.locatorForEntry(CONFIGURATION_INI_NAME, true);
			if (settingsFile != null) {
				final OutputStream output = loc.outputStreamForURL(settingsFile);
				if (output != null) {
					try {
						settings.saveTo(output, intrinsicSettings);
					} finally {
						try {
							output.close();
						} catch (final IOException e) {
							e.printStackTrace();
						}
					}
					break;
				}
			}
		}
	}

	public Collection<URL> getURLsOfStorageLocationPath(final String configurationFolder, final boolean onlyFromReadonlyStorageLocation) {
		final LinkedList<URL> result = new LinkedList<URL>();
		for (final IStorageLocation loc : storageLocations) {
			if (onlyFromReadonlyStorageLocation && loc.toFolder() != null)
				continue;
			loc.collectURLsOfContainer(configurationFolder, true, result);
		}
		return result;
	}

	public OutputStream outputStreamForStorageLocationEntry(final String entryPath) {
		for (final IStorageLocation loc : storageLocations) {
			final URL url = loc.locatorForEntry(entryPath, true);
			if (url != null) {
				final OutputStream result = loc.outputStreamForURL(url);
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

	public Variable[] variablesWithPrefix(final String prefix) {
		// FIXME: oh noes, will return array stored in map, making it possible to modify it
		if (cachedPrefixedVariables != null) {
			final Variable[] inCache = cachedPrefixedVariables.get(prefix);
			if (inCache != null)
				return inCache;
		}
		final List<Variable> result = new LinkedList<Variable>();
		for (final Variable v : variables())
			if (v.scope() == Scope.CONST && v.name().startsWith(prefix))
				result.add(v);
		final Variable[] resultArray = result.toArray(new Variable[result.size()]);
		if (cachedPrefixedVariables == null)
			cachedPrefixedVariables = new HashMap<String, Variable[]>();
		cachedPrefixedVariables.put(prefix, resultArray);
		return resultArray;
	}

	public Process executeEmbeddedUtility(final String name, final String... args) {
		if (!settings().supportsEmbeddedUtilities)
			return null;
		final String path = settings().engineExecutablePath;
		if (path != null) {
			final String[] completeArgs = new String[2+args.length];
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
							final IOConsoleOutputStream stream = Console.clonkConsole().newOutputStream();
							final byte[] buffer = new byte[1024];
							int bytesRead;
							while ((bytesRead = p.getInputStream().read(buffer)) > 0)
								stream.write(buffer, 0, bytesRead);
						} catch (final IOException e) {
							e.printStackTrace();
						}
					};
				}.start();
				return p;
			} catch (final IOException e) {
				System.out.println("Failed to execute utility " + name); //$NON-NLS-1$
				return null;
			}
		} else
			return null;
	}

	public C4Group.GroupType groupTypeForExtension(final String ext) {
		final C4Group.GroupType gt = settings.fileExtensionToGroupTypeMapping().get(ext);
		if (gt != null)
			return gt;
		else
			return C4Group.GroupType.OtherGroup;
	}

	public C4Group.GroupType groupTypeForFileName(final String fileName) {
		return groupTypeForExtension(fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase()); //$NON-NLS-1$
	}

	public Object image(final String name, final boolean returnDescriptor) {
		final Collection<URL> urls = getURLsOfStorageLocationPath("images", false); //$NON-NLS-1$
		if (urls != null)
			for (final URL url : urls)
				if (url.getFile().endsWith(name+".png")) //$NON-NLS-1$
					return returnDescriptor ? UI.imageDescriptorForURL(url) :  UI.imageForURL(url);
		return null;
	}

	public Image image(final GroupType groupType) {return (Image) image(groupType.name(), false);}
	public ImageDescriptor imageDescriptor(final GroupType groupType) {return (ImageDescriptor) image(groupType.name(), true);}
	public Image image(final String name) {return (Image) image(name, false);}
	public ImageDescriptor imageDescriptor(final String name) {return (ImageDescriptor) image(name, true);}

	/**
	 * Construct group name based on the name without extension and a {@link GroupType}
	 * @param name The name without extension
	 * @param groupType The group type
	 * @return Group name with correct extension.
	 */
	public String groupName(final String name, final GroupType groupType) {
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

	public boolean supportsPrimitiveType(final PrimitiveType type) {
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

	@Override
	public Typing typing() { return Typing.STATIC; }

}
