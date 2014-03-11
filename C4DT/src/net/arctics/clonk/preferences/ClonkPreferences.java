package net.arctics.clonk.preferences;

import net.arctics.clonk.Core;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
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
	public static final String TASKEXECUTION_THREADS = "taskExecutionThreads";

	// defaults
	public static final String ACTIVE_ENGINE_DEFAULT = "ClonkRage"; //$NON-NLS-1$
	public static final String PREFERRED_LANGID_DEFAULT = "DE"; //$NON-NLS-1$
	public static final String AUTHOR_DEFAULT = "<Insert Author>";

	public static String value(final String prefName) {
		return Core.instance().getPreferenceStore().getString(prefName);
	}

	public static boolean toggle(final String toggleName, final boolean defaultValue) {
		return Core.instance().getPreferenceStore().getBoolean(toggleName);
	}

	public static int integer(final String prefName) {
		return Core.instance().getPreferenceStore().getInt(prefName);
	}

	public static String languagePref() { return Core.instance() != null && Core.instance().getPreferenceStore() != null ? value(PREFERRED_LANGID) : PREFERRED_LANGID_DEFAULT; }

	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = Core.instance().getPreferenceStore();
		store.setDefault(ACTIVE_ENGINE, ACTIVE_ENGINE_DEFAULT);
		store.setDefault(SHOW_ERRORS_WHILE_TYPING, true);
		store.setDefault(STRUCTURE_OUTLINES_IN_PROJECT_EXPLORER, false);
		store.setDefault(ANALYZE_CODE, true);
		store.setDefault(INSTANT_C4SCRIPT_COMPLETIONS, false);
		store.setDefault(LONG_PARAMETER_INFO, true);
		store.setDefault(TASKEXECUTION_THREADS, Runtime.getRuntime().availableProcessors());
	}

}
