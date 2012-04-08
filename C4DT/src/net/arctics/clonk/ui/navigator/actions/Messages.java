package net.arctics.clonk.ui.navigator.actions;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.ui.navigator.actions.messages"; //$NON-NLS-1$
	public static String CustomizationDynamicMenuItem_SelectTopLevelEngineFolder;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
