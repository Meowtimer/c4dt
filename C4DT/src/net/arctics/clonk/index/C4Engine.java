package net.arctics.clonk.index;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.util.Util;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.c4script.Keywords;
import net.arctics.clonk.parser.inireader.CustomIniUnit;
import net.arctics.clonk.parser.inireader.IEntryFactory;
import net.arctics.clonk.parser.inireader.IniData;
import net.arctics.clonk.parser.inireader.IniEntry;
import net.arctics.clonk.parser.inireader.IniItem;
import net.arctics.clonk.parser.inireader.IniParserException;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.parser.inireader.IniData.IniDataSection;
import net.arctics.clonk.parser.inireader.IniField;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.IStorageLocation;
import net.arctics.clonk.util.LineNumberObtainer;
import net.arctics.clonk.util.Utilities;

/**
 * Container for engine functions and constants.
 * @author Madeen
 *
 */
public class C4Engine extends C4ScriptBase {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public static class EngineSettings implements Cloneable, Serializable {

		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

		@IniField
		public int strictDefaultLevel;
		@IniField
		public int maxStringLen;
		@IniField
		public String idPattern;
		@IniField
		public String docURLTemplate;
		@IniField
		public String engineExecutablePath;
		@IniField
		public String gamePath;
		@IniField
		public String repositoryPath;
		@IniField
		public String c4GroupPath;
		@IniField
		public boolean colonIDSyntax;
		@IniField
		public boolean nonConstGlobalVarsAssignment;
		@IniField
		public boolean definitionsHaveStaticVariables;
		@IniField
		public boolean supportsRefs;
		@IniField
		public boolean supportsDebugging;
		@IniField
		public String editorCmdLineOption;
		@IniField
		public String cmdLineOptionFormat;
		@IniField
		public String cmdLineOptionWithArgumentFormat;
		@IniField
		public boolean supportsEmbeddedUtilities;

		public EngineSettings() {
			try {
				for (Field f : getClass().getFields()) {
					if (f.getType() == String.class) {
						f.set(this, "");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public static final IniConfiguration INI_CONFIGURATION = IniConfiguration.createFromClass(EngineSettings.class);

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (!(obj instanceof EngineSettings))
				return false;
			for (Field f : getClass().getFields()) {
				try {
					Object fVal = f.get(this);
					Object objVal = f.get(obj);
					if (!Utilities.objectsEqual(fVal, objVal))
						return false;
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
			return true;
		}

		public void loadFrom(InputStream stream) {
			CustomIniUnit unit = new CustomIniUnit(stream);
			unit.setConfiguration(INI_CONFIGURATION);
			try {
				unit.parseAndCommitTo(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public void saveTo(OutputStream stream, EngineSettings defaults) {
			try {
				CustomIniUnit unit = new CustomIniUnit(INI_CONFIGURATION, (Object)this, defaults);
				Writer writer = new OutputStreamWriter(stream);
				try {
					unit.save(writer);
				} finally {
					writer.close();
				}
			} catch (Exception e) {
				e.printStackTrace(); 
			}
		}

		public static EngineSettings createFrom(InputStream stream) {
			EngineSettings settings = new EngineSettings();
			settings.loadFrom(stream);
			return settings;
		}

		@Override
		public EngineSettings clone() {
			try {
				return (EngineSettings)super.clone();
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}

	}

	private transient CachedEngineFuncs cachedFuncs;
	private transient Map<String, C4Variable[]> cachedPrefixedVariables;

	private transient EngineSettings intrinsicSettings;
	private transient EngineSettings currentSettings;
	
	private transient IStorageLocation[] storageLocations;
	private transient IniData iniConfigurations;
	private transient Map<String, Map<String, String>> descriptions = new HashMap<String, Map<String,String>>();

	public EngineSettings getIntrinsicSettings() {
		return intrinsicSettings;
	}

	public void setCurrentSettings(EngineSettings currentSettings) {
		this.currentSettings = currentSettings;
	}

	public EngineSettings getCurrentSettings() {
		return currentSettings;
	}

	public final CachedEngineFuncs getCachedFuncs() {
		return cachedFuncs;
	}
	
	public IniData getIniConfigurations() {
		return iniConfigurations;
	}

	public C4Engine(String name) {
		super();
		setName(name);
		modified();
	}

	public void modified() {
		if (intrinsicSettings == null) {
			intrinsicSettings = new EngineSettings();
		}
		if (currentSettings == null) {
			currentSettings = new EngineSettings();
		}
		cachedFuncs = new CachedEngineFuncs(this);
		cachedPrefixedVariables = null;
	}

	@Override
	public void postSerialize(C4Declaration parent) {
		super.postSerialize(parent);
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
			}; //$NON-NLS-1$
		}
		// assume some UNIX -.-
		return new String[] { "clonk" }; //$NON-NLS-1$
	}

	public boolean acceptsId(String text) {
		return currentSettings.idPattern == null || text.matches(currentSettings.idPattern);
	}

	public boolean hasCustomSettings() {
		return !currentSettings.equals(intrinsicSettings);
	}

	@Override
	public C4Engine getEngine() {
		return this;
	}

	@Override
	public IFile getScriptStorage() {
		return null;
	}

	@Override
	public ClonkIndex getIndex() {
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
						intrinsicSettings = EngineSettings.createFrom(input);
						currentSettings = intrinsicSettings.clone();
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
			currentSettings = intrinsicSettings.clone();
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
	
	private class DescriptionsIniConfiguration extends IniConfiguration {
		public DescriptionsIniConfiguration() {
			super();
			sections.put("Descriptions", new IniDataSection() { //$NON-NLS-1$
				private IniDataEntry entry = new IniDataEntry("", String.class); //$NON-NLS-1$
				@Override
				public boolean hasEntry(String entryName) {
					return C4Engine.this.findDeclaration(entryName) != null; 
				}
				@Override
				public IniDataEntry getEntry(String key) {
					return entry;
				}
			});
			factory = new IEntryFactory() {
				@Override
				public Object create(Class<?> type, String value, IniDataEntry entryData, IniUnit context) throws InvalidClassException, IniParserException {
					return value;
				}
			};
		}
	}
	
	private class DeclarationsConfiguration extends IniConfiguration {
		@Override
		public boolean hasSection(String sectionName) {
			return C4Engine.this.findDeclaration(sectionName) != null;
		}
	}

	public String descriptionFor(C4Declaration declaration) {
		Map<String, String> descs;
		try {
			descs = getEngine().loadDescriptions(ClonkPreferences.getLanguagePref());
			return descs != null ? descs.get(declaration.getName()) : null;
		} catch (IOException e) {
			return null;
		}
	}
	
	public Map<String, String> loadDescriptions(String language) throws IOException {
		Map<String, String> result = descriptions.get(language);
		if (result != null)
			return result;
		else {
			String fileName = String.format("descriptions%s.ini", language); //$NON-NLS-1$
			for (int i = storageLocations.length-1; i >= 0; i--) {
				IStorageLocation loc = storageLocations[i];
				URL descs = loc.getURL(fileName, false);
				if (descs != null) {
					InputStream input = descs.openStream();
					try {
						IniUnit unit = new CustomIniUnit(input, new DescriptionsIniConfiguration());
						unit.parse(false);
						IniSection section = unit.sectionWithName("Descriptions"); //$NON-NLS-1$
						if (section != null) {
							result = new HashMap<String, String>();
							for (Entry<String, IniItem> item : section.getSubItemMap().entrySet()) {
								if (item.getValue() instanceof IniEntry) {
									IniEntry entry = (IniEntry) item.getValue();
									result.put(entry.getKey(), entry.getValue().replace("|||", "\n")); //$NON-NLS-1$ //$NON-NLS-2$
								}
							}
							descriptions.put(language, result);
							return result;
						}
					} finally {
						input.close();
					}
				}
			}
		}
		return null;
	}
	
	public void loadDeclarationsConfiguration() throws NoSuchFieldException, IllegalAccessException, IOException {
		for (int i = storageLocations.length-1; i >= 0; i--) {
			IStorageLocation loc = storageLocations[i];
			if (loc == null)
				continue;
			URL url = loc.getURL("declarations.ini", false);
			if (url != null) {
				InputStream stream = url.openStream();
				try {
					CustomIniUnit unit = new CustomIniUnit(stream, new DeclarationsConfiguration());
					unit.parse(false);
					for (IniSection section : unit.getSections()) {
						C4Declaration declaration = findDeclaration(section.getName());
						if (declaration != null) {
							unit.commitSection(declaration, section, false);
						}
					}
				} finally {
					stream.close();
				}
			}
		}
	}
	
	public void parseEngineScript(final URL url) throws IOException, ParsingException {
		InputStream stream = url.openStream();
		try {
			String scriptFromStream = Utilities.stringFromInputStream(stream);
			final LineNumberObtainer lno = new LineNumberObtainer(scriptFromStream);
			C4ScriptParser parser = new C4ScriptParser(scriptFromStream, this, null) {
				private boolean firstMessage = true;
				@Override
				public IMarker markerWithCode(ParserErrorCode code,
						int errorStart, int errorEnd, boolean noThrow,
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
					return super.markerWithCode(code, errorStart, errorEnd, noThrow, severity, args);
				}
			};
			parser.parse();
			postSerialize(null);
		} finally {
			stream.close();
		}
	}
	
	public static C4Engine loadFromStorageLocations(final IStorageLocation... providers) {
		C4Engine result = null;
		try {
			for (IStorageLocation location : providers) {
				URL url = location.getURL(location.getName()+".c", false); //$NON-NLS-1$
				if (url != null) {
					result = new C4Engine(location.getName());
					result.storageLocations = providers;
					result.loadSettings();
					result.loadIniConfigurations();
					result.parseEngineScript(url);
					result.loadDeclarationsConfiguration();
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public void writeEngineScript(Writer writer) throws IOException {
		for (C4Variable v : variables()) {
			String text = String.format("%s %s;\n", v.getScope().toKeyword(), v.getName()); //$NON-NLS-1$
			writer.append(text);
		}
		writer.append("\n"); //$NON-NLS-1$
		for (C4Function f : functions()) {
			String returnType = f.getReturnType().toString();
			String desc = f.getUserDescription();
			if (desc != null) {
				if (desc.contains("\n")) { //$NON-NLS-1$
					desc = String.format("/*\n%s\n*/\n", desc); //$NON-NLS-1$
				} else {
					desc = String.format("//%s\n", desc); //$NON-NLS-1$
				}
			} else {
				desc = ""; //$NON-NLS-1$
			}
			String text = String.format("%s %s %s %s;\n", f.getVisibility().toKeyword(), Keywords.Func, returnType, f.getLongParameterString(true, true)); //$NON-NLS-1$
			writer.append(text);
		}
	}
	
	public void writeEngineScript() throws IOException {
		for (IStorageLocation loc : storageLocations) {
			URL scriptFile = loc.getURL(loc.getName()+".c", true);
			if (scriptFile != null) {
				OutputStream output = loc.getOutputStream(scriptFile);
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
	
	public void saveSettings() throws IOException {
		if (!hasCustomSettings())
			return;
		for (IStorageLocation loc : storageLocations) {
			URL settingsFile = loc.getURL(CONFIGURATION_INI_NAME, true);
			if (settingsFile != null) {
				OutputStream output = loc.getOutputStream(settingsFile);
				if (output != null) {
					try {
						currentSettings.saveTo(output, intrinsicSettings);
					} finally {
						output.close();
					}
					break;
				}
			}
		}
	}
	
	public Enumeration<URL> getURLsOf(String configurationFolder) {
		for (IStorageLocation loc : storageLocations) {
			Enumeration<URL> result = loc.getURLs(configurationFolder);
			if (result.hasMoreElements())
				return result;
		}
		return null;
	}
	
	public C4Variable[] variablesWithPrefix(String prefix) {
		// FIXME: oh noes, will return array stored in map, making it possible to modify it
		if (cachedPrefixedVariables != null) {
			C4Variable[] inCache = cachedPrefixedVariables.get(prefix);
			if (inCache != null)
				return inCache;
		}
		List<C4Variable> result = new LinkedList<C4Variable>();
		for (C4Variable v : variables()) {
			if (v.getScope() == C4VariableScope.CONST && v.getName().startsWith(prefix)) {
				result.add(v);
			}
		}
		C4Variable[] resultArray = result.toArray(new C4Variable[result.size()]);
		if (cachedPrefixedVariables == null)
			cachedPrefixedVariables = new HashMap<String, C4Variable[]>();
		cachedPrefixedVariables.put(prefix, resultArray);
		return resultArray;
	}
	
	public Process executeEmbeddedUtility(String name, String... args) {
		if (!getCurrentSettings().supportsEmbeddedUtilities)
			return null;
		String path = getCurrentSettings().engineExecutablePath;
		if (path != null) {
			String[] completeArgs = new String[2+args.length];
			completeArgs[0] = path;
			completeArgs[1] = "-x"+name;
			for (int i = 0; i < args.length; i++)
				completeArgs[2+i] = args[i];
			try {
				return Runtime.getRuntime().exec(completeArgs);
			} catch (IOException e) {
				System.out.println("Failed to execute utility " + name);
				return null;
			}
		} else {
			return null;
		}
	}

}
