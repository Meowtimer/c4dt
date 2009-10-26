package net.arctics.clonk.preferences;

import net.arctics.clonk.ClonkCore;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IScopeContext;

/**
 * Constant definitions for clonk preferences
 */
public class PreferenceConstants {
	public static final String GAME_PATH = "gamePath";
	public static final String STANDARD_EXT_LIBS = "standardExtLibs";
	public static final String C4GROUP_EXECUTABLE = "c4groupExecutable";
	public static final String SHOW_EXPORT_LOG = "showExportLog";
	public static final String PREFERRED_LANGID = "preferredLangID";
	public static final String ENGINE_EXECUTABLE = "engineExecutable";	
	public static final String OPENCLONK_REPO = "openClonkRepo";
	public static final String DOC_URL_TEMPLATE = "docURLTemplate";
	public static final String EXTERNAL_INDEX_ENCODING = "externalIndexEncoding";
	
	public static final String EXTERNAL_INDEX_ENCODING_DEFAULT = "ISO-8859-1";
	public static final String DOC_URL_TEMPLATE_DEFAULT = "http://www.clonk.de/docs/%2$s/sdk/script/fn/%1$s.html";
	
	public static String getPreference(String prefName, String def, IScopeContext[] contexts) {
		return Platform.getPreferencesService().getString(ClonkCore.PLUGIN_ID, prefName, def, contexts);
	}
	
	public static String getPreference(String prefName) {
		return getPreference(prefName, null, null);
	}
}
