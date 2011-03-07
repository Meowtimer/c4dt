package net.arctics.clonk.parser.c4script.specialscriptrules;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.parser.c4script.specialscriptrules.messages"; //$NON-NLS-1$
	public static String SpecialScriptRules_OpenClonk_ActionCompletionTemplate;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
