package net.arctics.clonk.parser.c4script;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.parser.c4script.messages"; //$NON-NLS-1$
	public static String DescriptionNotAvailable;
	public static String C4Function_InfoTextTemplate;
	public static String TokenStringOrIdentifier;
	public static String InternalError_WayTooMuch;
	public static String InternalParserError;
	public static String This_Description;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
