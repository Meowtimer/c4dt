package net.arctics.clonk.preferences;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.Core;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Constant definitions for clonk preferences
 */
public class ClonkPreferences extends AbstractPreferenceInitializer {
	
	// options that are actually stored in the default pref store
	public static final String ACTIVE_ENGINE = "selectedEngine"; //$NON-NLS-1$
	public static final String SHOW_EXPORT_LOG = "showExportLog"; //$NON-NLS-1$
	public static final String PREFERRED_LANGID = "preferredLangID"; //$NON-NLS-1$
	public static final String OPEN_EXTERNAL_BROWSER = "openExternalBrowser"; //$NON-NLS-1$
	public static final String SHOW_ERRORS_WHILE_TYPING = "showErrorsWhileTyping";
	public static final String STRUCTURE_OUTLINES_IN_PROJECT_EXPLORER = "showOutlinesInProjectExplorer";
	public static final String NO_AUTOBRACKETPAIRS = "noAutoBracketPairs";
	public static final String AUTHOR = "author";
	public static final String IGNORE_SIMPLE_FUNCTION_DUPES = "ignoreSimpleFunctionDupes"; //$NON-NLS-1$
	public static final String ANALYZE_CODE = "analyzeCode";
	public static final String JAVA_STYLE_BLOCKS = "javaStyleBlocks";
	
	// defaults
	public static final String ACTIVE_ENGINE_DEFAULT = "ClonkRage"; //$NON-NLS-1$
	public static final String PREFERRED_LANGID_DEFAULT = "DE"; //$NON-NLS-1$
	public static final String AUTHOR_DEFAULT = "<Insert Author>";
	
	private static final Map<String, Field> valueFieldMapping = new HashMap<String, Field>();
	
	public static String valueOrDefault(String prefName) {
		String def;
		try {
			Field prefField = valueFieldMapping.get(prefName);
			if (prefField == null)
				for (Field f : ClonkPreferences.class.getFields())
					if (!f.getName().endsWith("_DEFAULT"))
						if (f.get(null).equals(prefName)) {
							prefField = f;
							valueFieldMapping.put(prefName, f);
							break;
						}
	        Field f = prefField != null ? ClonkPreferences.class.getField(prefField.getName()+"_DEFAULT") : null; //$NON-NLS-1$
	        def = f != null ? f.get(null).toString() : null;
        } catch (Exception e) {
	        def = null;
        }
        return value(prefName, def, null);
	}
	
	public static String value(String prefName, String def, IScopeContext[] contexts) {
		try {
			return Platform.getPreferencesService().getString(Core.PLUGIN_ID, prefName, def, contexts);
		} catch (Exception e) {
			return def;
		}
	}
	
	public static String value(String prefName) {
		return value(prefName, null, null);
	}
	
	public static boolean toggle(String toggleName, boolean defaultValue) {
		return Platform.getPreferencesService().getBoolean(Core.PLUGIN_ID, toggleName, defaultValue, null);
	}
	
	public static String languagePref() {
		return valueOrDefault(PREFERRED_LANGID);
	}
	
	public static String getLanguagePrefForDocumentation() {
		String pref = languagePref();
		return pref.equals("DE") ? "de" : "en";
	}
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Core.instance().getPreferenceStore();
		store.setDefault(ACTIVE_ENGINE, ACTIVE_ENGINE_DEFAULT);
		store.setDefault(SHOW_ERRORS_WHILE_TYPING, true);
		store.setDefault(STRUCTURE_OUTLINES_IN_PROJECT_EXPLORER, true);
		store.setDefault(ANALYZE_CODE, true);
	}
	
}
