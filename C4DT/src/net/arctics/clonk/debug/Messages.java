package net.arctics.clonk.debug;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackage().getName()+".messages"; //$NON-NLS-1$
	public static String ProjectNotOpen;
	public static String ScenarioNotFound;
	public static String MainThread;
	public static String ClonkDebugLineBreakpoint_BreakpointMessage;
	public static String ClonkDebugStackFrame_StackFrameMessage;
	public static String ClonkLaunchConfigurationDelegate_NoScenario;
	public static String ClonkLaunchConfigurationDelegate_SomethingWentWrong;
	public static String CouldNotFindEngine;
	public static String LaunchConf;
	public static String StartingClonkEngine;
	public static String CouldNotStartEngine;
	public static String EngineDoesNotSupportDebugging;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
