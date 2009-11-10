package net.arctics.clonk.preferences;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

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
	public static final String ACTIVE_ENGINE = "selectedEngine"; //$NON-NLS-1$
	
	public static final String EXTERNAL_INDEX_ENCODING_DEFAULT = "ISO-8859-1"; //$NON-NLS-1$
	public static final String DOC_URL_TEMPLATE_DEFAULT = Messages.DocURLTemplateDefault;
	public static final String ACTIVE_ENGINE_DEFAULT = "ClonkRage"; //$NON-NLS-1$
	public static final String PREFERRED_LANGID_DEFAULT = "DE"; //$NON-NLS-1$
	
	private static final Map<String, Field> valueFieldMapping = new HashMap<String, Field>();
	
	public static String getPreferenceOrDefault(String prefName) {
		String def;
		try {
			Field prefField = valueFieldMapping.get(prefName);
			if (prefField == null) {
				for (Field f : ClonkPreferences.class.getFields()) {
					if (!f.getName().endsWith("_DEFAULT")) { //$NON-NLS-1$
						if (f.get(null).equals(prefName)) {
							prefField = f;
							valueFieldMapping.put(prefName, f);
							break;
						}
					}
				}
			}
	        Field f = prefField != null ? ClonkPreferences.class.getField(prefField.getName()+"_DEFAULT") : null; //$NON-NLS-1$
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
	
	public static String getLanguagePref() {
		return getPreferenceOrDefault(PREFERRED_LANGID);
	}
}
