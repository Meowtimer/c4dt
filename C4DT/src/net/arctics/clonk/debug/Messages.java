package net.arctics.clonk.debug;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.debug.messages"; //$NON-NLS-1$
	public static String ProjectNotOpen;
	public static String ScenarioNotFound;
	public static String ClonkDebugThread_0;
	public static String CouldNotFindEngine;
	public static String LauncherOnlySupportsRunMode;
	public static String LaunchConf;
	public static String StartingClonkEngine;
	public static String CouldNotStartEngine;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
