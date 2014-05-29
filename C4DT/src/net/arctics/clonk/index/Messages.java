package net.arctics.clonk.index;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackage().getName()+".messages"; //$NON-NLS-1$
	public static String DefinitionIDWithName;
	public static String DefinitionInfoTextTemplate;
	public static String Engine_FetchDocumentationInfoJobTask;
	public static String Engine_FetchingDocumentationInfoJobTitle;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
