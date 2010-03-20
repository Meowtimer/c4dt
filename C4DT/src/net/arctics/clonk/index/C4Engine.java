package net.arctics.clonk.index;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.EnumSet;
import java.util.Enumeration;

import org.eclipse.jface.util.Util;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.inireader.CustomIniUnit;
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

	public enum EngineCapability {
		ColonIDSyntax,
		NonConstGlobalVarsAssignment
	}

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
		public EnumSet<EngineCapability> capabilities;

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

	public boolean hasCapability(EngineCapability cap) {
		return currentSettings.capabilities != null && currentSettings.capabilities.contains(cap);
	}

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
		/*try {
			for (IStorageLocation provider : providers) {
				URL url = provider.getURL(provider.getName()+".engine", false);
				if (url != null) {
					engineStream = url.openStream();
					ObjectInputStream objStream = new InputStreamRespectingUniqueIDs(engineStream);
					result = (C4Engine)objStream.readObject();
					result.setName(provider.getName()); // for good measure
					result.postSerialize(null);
					result.storageLocations = providers;
					break;
				}
			}
		} catch (Exception e) {
			// fallback to xml
			ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
			try {
				IRunnableWithProgressAndResult<C4Engine> xmlImportor = new IRunnableWithProgressAndResult<C4Engine>() {
					private C4Engine engine;
					
					@Override
                    public C4Engine getResult() {
	                    return engine;
                    }

					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						for (IStorageLocation provider : providers) {
							URL url = provider.getURL(provider.getName()+".engine.xml", false);
							if (url != null) {
								engine = new C4Engine(provider.getName());
								try {
									engine.importFromXML(url.openStream(), monitor); //$NON-NLS-1$
									engine.storageLocations = providers;
								} catch (Exception e) {
									e.printStackTrace();
								}
								break;
							}
						}
                    }
				};
	            progressDialog.run(false, false, xmlImportor);
	            result = xmlImportor.getResult();
            } catch (Exception e1) {
	            e1.printStackTrace();
            }
		}*/
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

}
