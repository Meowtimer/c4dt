package net.arctics.clonk.parser.inireader;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.parser.inireader.messages"; //$NON-NLS-1$
	public static String UnknownConstant;
	public static String UnknownOption;
	public static String IniSection_InfoTextFormat;
	public static String InternalIniParserBug;
	public static String ExpectedIntegerArray;
	public static String IntegerExpected;
	public static String OnlyUnsignedIntegersAllowed;
	public static String UnsignedValueExpected;
	public static String InvalidValue;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
