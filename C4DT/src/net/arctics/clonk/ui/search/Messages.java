package net.arctics.clonk.ui.search;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackage().getName()+".messages"; //$NON-NLS-1$
	public static String C4ScriptSearchPage_Recents;
	public static String C4ScriptSearchPage_Replacement;
	public static String C4ScriptSearchPage_Template;
	public static String ClonkSearchQuery_SearchFor;
	public static String ClonkSearchQuery_Success;
	public static String FindDuplicatesMatch_PotentialDuplicateLabel;
	public static String FindDuplicatesQuery_Label;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
