package net.arctics.clonk.command.ui.actions;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackage().getName()+".messages"; //$NON-NLS-1$
	public static String InvokeCommandAction_InvokeCommand;
	public static String InvokeCommandAction_SpecifyCommand;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
