package net.arctics.clonk.util;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackage().getName()+".messages"; //$NON-NLS-1$
	
	public static String UI_Confirm;
	public static String Utilities_ClonkConsole;
	public static String Utilities_DebugConsole;
	public static String Utilities_InternalError;
	public static String Utilities_InternalErrorButton;
	public static String Utilities_RefreshingProjects;
	public static String Utilities_ChooseClonkProject;
	public static String Utilities_ChooseClonkProjectPretty;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
