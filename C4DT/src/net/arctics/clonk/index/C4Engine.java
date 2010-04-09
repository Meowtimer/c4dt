package net.arctics.clonk.index;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.util.Util;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.inireader.CustomIniUnit;
import net.arctics.clonk.parser.inireader.IniData;
import net.arctics.clonk.parser.inireader.IniField;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;
import net.arctics.clonk.util.IStorageLocation;
import net.arctics.clonk.util.Utilities;

/**
 * Container for engine functions and constants.
 * @author Madeen
 *
 */
public class C4Engine extends C4ScriptBase {

	private static final long serialVersionUID = 1L;

	public static class EngineSettings implements Cloneable, Serializable {

		private static final long serialVersionUID = 1L;

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
					unit.write(writer);
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

	private transient EngineSettings intrinsicSettings;
	private transient EngineSettings currentSettings;
	private transient IStorageLocation[] storageLocations;
	private transient IniData iniConfigurations;

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
	}

	public void modified() {
		if (intrinsicSettings == null) {
			intrinsicSettings = new EngineSettings();
		}
		cachedFuncs = new CachedEngineFuncs(this);
	}

	@Override
	public void postSerialize(C4Declaration parent) {
		super.postSerialize(parent);
		modified();
	}

	public static String[] possibleEngineNamesAccordingToOS() {
		if (Util.isMac()) {
			return new String[] { "Clonk.app/Contents/MacOS/Clonk" }; //$NON-NLS-1$
		}
		if (Util.isLinux()) {
			return new String[] { "clonk" }; //$NON-NLS-1$
		}
		if (Util.isWindows()) {
			return new String[] { "Clonk.c4x", "Clonk.exe" }; //$NON-NLS-1$ //$NON-NLS-2$
		}
		// assume some UNIX -.-
		return new String[] { "clonk" };
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
	public Object getScriptFile() {
		return new Object();
	}

	@Override
	public ClonkIndex getIndex() {
		return null;
	}
	
	private static final String configurationIniName = "configuration.ini";
	
	public void loadSettings() throws IOException {
		// combine settings files in reverse-order so custom config is based on default
		for (int i = storageLocations.length-1; i >= 0; i--) {
			IStorageLocation loc = storageLocations[i];
			URL settingsFile = loc.getURL(configurationIniName, false);
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
	
	public void saveSettings() throws IOException {
		if (!hasCustomSettings())
			return;
		for (IStorageLocation loc : storageLocations) {
			URL settingsFile = loc.getURL(configurationIniName, true);
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

	public static C4Engine loadFromStorageLocations(final IStorageLocation... providers) {
		C4Engine result = null;
		try {
			for (IStorageLocation location : providers) {
				URL url = location.getURL(location.getName()+".c", false);
				if (url != null) {
					result = new C4Engine(location.getName());
					result.storageLocations = providers;
					result.loadSettings();
					result.loadIniConfigurations();
					InputStream stream = url.openStream();
					try {
						C4ScriptParser parser = new C4ScriptParser(Utilities.stringFromInputStream(stream), result);
						parser.parse();
						result.postSerialize(null);
					} finally {
						stream.close();
					}
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
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
		List<C4Variable> result = new LinkedList<C4Variable>();
		for (C4Variable v : variables()) {
			if (v.getScope() == C4VariableScope.VAR_CONST && v.getName().startsWith(prefix)) {
				result.add(v);
			}
		}
		return result.toArray(new C4Variable[result.size()]);
	}

}
