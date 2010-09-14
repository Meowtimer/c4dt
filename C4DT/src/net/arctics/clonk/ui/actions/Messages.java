package net.arctics.clonk.ui.actions;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.ui.actions.messages"; //$NON-NLS-1$
	public static String AboutHandler_InfoTemplate;
	public static String AboutHandler_Title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
