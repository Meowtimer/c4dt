package net.arctics.clonk.c4script.specialenginerules;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.c4script.specialEngineRules.messages"; //$NON-NLS-1$
	public static String specialEngineRules_OpenClonk_ActionCompletionTemplate;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
