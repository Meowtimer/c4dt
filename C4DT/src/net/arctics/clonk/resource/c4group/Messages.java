package net.arctics.clonk.resource.c4group;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.resource.c4group.messages"; //$NON-NLS-1$
	public static String NoImportingToWorkspaceRoot;
	public static String WhereToSave;
	public static String ExportC4GroupJobTitle;
	public static String Exporting;
	public static String ExporterCommandlineTitle;
	public static String C4Group_Copying;
	public static String C4GroupHeaderInvalid_1;
	public static String C4GroupHeaderInvalid_2;
	public static String C4GroupHeaderSuspicious;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
