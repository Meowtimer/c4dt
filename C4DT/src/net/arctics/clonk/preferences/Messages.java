package net.arctics.clonk.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.preferences.messages"; //$NON-NLS-1$
	public static String EngineVersion;
	public static String ClonkPreferencePage_GroupFileOrFolder;
	public static String ClonkPreferencePage_Nope;
	public static String ClonkPreferencePage_SelectExternalFolder;
	public static String ClonkPreferencePage_SelectRegularFolder;
	public static String ClonkPreferencePage_YesIndeed;
	public static String ClonkPreferences;
	public static String GamePath;
	public static String Finnish;
	public static String DocumentURLTemplate;
	public static String ExternalObjectsAndScripts;
	public static String ChooseExternalObject;
	public static String EncodingForExternalObjects;
	public static String ShowExportLog;
	public static String C4GroupExecutable;
	public static String EngineExecutable;
	public static String OpenClonkRepo;
	public static String PreferredLangID;
	public static String Project_ShowDependencies;
	public static String German;
	public static String USEnglish;
	public static String DocURLTemplateDefault;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
