package net.arctics.clonk;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.messages"; //$NON-NLS-1$
	public static String ClonkCore_ErrorWhileSavingIndex;
	public static String ClonkCore_ErrorWhileSavingSettings;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
