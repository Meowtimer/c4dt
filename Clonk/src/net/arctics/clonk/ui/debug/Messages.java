package net.arctics.clonk.ui.debug;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.ui.debug.messages"; //$NON-NLS-1$
	public static String LaunchMainTab_ProjectTitle;
	public static String LaunchMainTab_Browse;
	public static String LaunchMainTab_InvalidProjectName;
	public static String LaunchMainTab_ProjectDoesNotExist;
	public static String LaunchMainTab_ProjectNotOpen;
	public static String LaunchMainTab_ScenarioTitle;
	public static String LaunchMainTab_ScenarioNameInvalid;
	public static String LaunchMainTab_ScenarioDoesNotExist;
	public static String LaunchMainTab_ChooseClonkProject;
	public static String LaunchMainTab_ChooseClonkProjectPretty;
	public static String LaunchMainTab_ChooseClonkScenario;
	public static String LaunchMainTab_ChooseClonkScenarioPretty;
	public static String LaunchMainTab_LaunchMode;
	public static String LaunchMainTab_Console;
	public static String LaunchMainTab_Fullscreen;
	public static String LaunchMainTab_CreateRecord;
	public static String LaunchMainTab_Main;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
