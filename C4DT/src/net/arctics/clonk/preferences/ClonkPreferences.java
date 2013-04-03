package net.arctics.clonk.preferences;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.Core;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.IPreferencesService;
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
	public static final String INSTANT_C4SCRIPT_COMPLETIONS = "instantC4ScriptCompletions";
	public static final String LONG_PARAMETER_INFO = "longParameterInfo";
	
	// defaults
	public static final String ACTIVE_ENGINE_DEFAULT = "ClonkRage"; //$NON-NLS-1$
	public static final String PREFERRED_LANGID_DEFAULT = "DE"; //$NON-NLS-1$
	public static final String AUTHOR_DEFAULT = "<Insert Author>";
	
	private static final Map<String, Field> valueFieldMapping = new HashMap<String, Field>();
	
	public static String value(String prefName) {
        return value(prefName, ClonkPreferences.<String>defaultValue(prefName), null);
	}

	@SuppressWarnings("unchecked")
	protected static <T> T defaultValue(String prefName) {
		try {
			Field prefField = valueFieldMapping.get(prefName);
			if (prefField == null)
				for (final Field f : ClonkPreferences.class.getFields())
					if (!f.getName().endsWith("_DEFAULT"))
						if (f.get(null).equals(prefName)) {
							prefField = f;
							valueFieldMapping.put(prefName, f);
							break;
						}
			final Field f = prefField != null ? ClonkPreferences.class.getField(prefField.getName()+"_DEFAULT") : null; //$NON-NLS-1$
			return f != null ? (T)f.get(null) : null;
		} catch (final Exception e) {
			return null;
		}
	}
	
	public static String value(String prefName, String def, IScopeContext[] contexts) {
		try {
			return Platform.getPreferencesService().getString(Core.PLUGIN_ID, prefName, def, contexts);
		} catch (final Exception e) {
			return def;
		}
	}
	
	public static boolean toggle(String toggleName, boolean defaultValue) {
		final IPreferencesService prefService = Platform.getPreferencesService();
		return prefService != null ? prefService.getBoolean(Core.PLUGIN_ID, toggleName, defaultValue, null) : defaultValue;
	}
	
	public static String languagePref() {
		return value(PREFERRED_LANGID);
	}
	
	public static String getLanguagePrefForDocumentation() {
		final String pref = languagePref();
		return pref.equals("DE") ? "de" : "en";
	}
	
	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = Core.instance().getPreferenceStore();
		store.setDefault(ACTIVE_ENGINE, ACTIVE_ENGINE_DEFAULT);
		store.setDefault(SHOW_ERRORS_WHILE_TYPING, true);
		store.setDefault(STRUCTURE_OUTLINES_IN_PROJECT_EXPLORER, true);
		store.setDefault(ANALYZE_CODE, true);
		store.setDefault(INSTANT_C4SCRIPT_COMPLETIONS, false);
		store.setDefault(LONG_PARAMETER_INFO, true);
	}
	
}
