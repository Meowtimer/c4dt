package net.arctics.clonk.util;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.util.messages"; //$NON-NLS-1$
	public static String Utilities_0;
	public static String Utilities_1;
	public static String Utilities_12;
	public static String Utilities_13;
	public static String Utilities_9;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
