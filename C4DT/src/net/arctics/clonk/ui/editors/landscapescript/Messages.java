package net.arctics.clonk.ui.editors.landscapescript;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.ui.editors.landscapescript.messages"; //$NON-NLS-1$
	public static String LandscapeScriptSourceViewerConfiguration_Proposals;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
