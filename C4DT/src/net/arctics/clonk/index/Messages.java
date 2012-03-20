package net.arctics.clonk.index;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.index.messages"; //$NON-NLS-1$
	public static String DefinitionIDWithName;
	public static String DefinitionInfoTextTemplate;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
