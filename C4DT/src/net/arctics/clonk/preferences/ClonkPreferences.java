package net.arctics.clonk.preferences;

import java.lang.reflect.Field;

import net.arctics.clonk.ClonkCore;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IScopeContext;

/**
 * Constant definitions for clonk preferences
 */
public class ClonkPreferences {
	public static final String GAME_PATH = "gamePath"; //$NON-NLS-1$
	public static final String STANDARD_EXT_LIBS = "standardExtLibs"; //$NON-NLS-1$
	public static final String C4GROUP_EXECUTABLE = "c4groupExecutable"; //$NON-NLS-1$
	public static final String SHOW_EXPORT_LOG = "showExportLog"; //$NON-NLS-1$
	public static final String PREFERRED_LANGID = "preferredLangID"; //$NON-NLS-1$
	public static final String ENGINE_EXECUTABLE = "engineExecutable";	 //$NON-NLS-1$
	public static final String OPENCLONK_REPO = "openClonkRepo"; //$NON-NLS-1$
	public static final String DOC_URL_TEMPLATE = "docURLTemplate"; //$NON-NLS-1$
	public static final String EXTERNAL_INDEX_ENCODING = "externalIndexEncoding"; //$NON-NLS-1$
	public static final String SELECTED_ENGINE = "selectedEngine"; //$NON-NLS-1$
	
	public static final String EXTERNAL_INDEX_ENCODING_DEFAULT = "ISO-8859-1"; //$NON-NLS-1$
	public static final String DOC_URL_TEMPLATE_DEFAULT = Messages.DocURLTemplateDefault;
	public static final String SELECTED_ENGINE_DEFAULT = "ClonkRage"; //$NON-NLS-1$
	
	public static String getPreferenceOrDefault(String prefName) {
		String def;
		try {
	        Field f = ClonkPreferences.class.getField(prefName+"_DEFAULT");
	        def = f != null ? f.get(null).toString() : null;
        } catch (Exception e) {
	        def = null;
        }
        return getPreference(prefName, def, null);
	}
	
	public static String getPreference(String prefName, String def, IScopeContext[] contexts) {
		return Platform.getPreferencesService().getString(ClonkCore.PLUGIN_ID, prefName, def, contexts);
	}
	
	public static String getPreference(String prefName) {
		return getPreference(prefName, null, null);
	}
}
