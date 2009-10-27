package net.arctics.clonk.resource;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.resource.messages"; //$NON-NLS-1$
	public static String Cleaning_Up;
	public static String Build_Project;
	public static String Parsing_Libraries;
	public static String Saving_Libraries;
	public static String Index_Project;
	public static String Parse_Project;
	public static String Saving_Data;
	public static String External_Library_Does_Not_Exist;
	public static String Parsing_Libs;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
