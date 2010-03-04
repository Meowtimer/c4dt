package net.arctics.clonk.index;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.eclipse.jface.util.Util;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
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
		
		public int strictDefaultLevel;
		public int maxStringLen;
		public String idPattern;
		public String docURLTemplate;
		public String engineExecutablePath;
		public String gamePath;
		public String repositoryPath;
		public String c4GroupPath;

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
		
		@Override
		public EngineSettings clone() throws CloneNotSupportedException {
			return (EngineSettings)super.clone();
		}
		
	}
	
	private transient CachedEngineFuncs cachedFuncs;
	
	private EngineSettings intrinsicSettings;
	private transient EngineSettings currentSettings;
	
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
//    		intrinsicSettings.strictDefaultLevel = strictDefaultLevel;
//    		intrinsicSettings.maxStringLen = maxStringLen;
//    		intrinsicSettings.idPattern = idPattern;
//    		strictDefaultLevel = 0;
//    		maxStringLen = 0;
//    		idPattern = null;
    	}
    	cachedFuncs = new CachedEngineFuncs(this);
    }
    
    @Override
    public void postSerialize(C4Declaration parent) {
    	super.postSerialize(parent);
    	modified();
    	try {
			currentSettings = (EngineSettings) intrinsicSettings.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
    }
    
    public static String[] possibleEngineNamesAccordingToOS() {
		if (Util.isMac()) {
			return new String[] { "Clonk.app/Contents/MacOS/Clonk" }; //$NON-NLS-1$
		}
		if (Util.isLinux()) {
			return new String[] { "clonk" }; //$NON-NLS-1$
		}
		//if (Util.isWindows()) {
    		return new String[] { "Clonk.c4x", "Clonk.exe" }; //$NON-NLS-1$ //$NON-NLS-2$
    	//}
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

}
