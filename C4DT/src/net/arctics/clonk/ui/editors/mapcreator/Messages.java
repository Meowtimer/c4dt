package net.arctics.clonk.ui.editors.mapcreator;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.ui.editors.mapcreator.messages"; //$NON-NLS-1$
	public static String MapCreatorSourceViewerConfiguration_Proposals;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
