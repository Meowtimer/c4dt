package net.arctics.clonk.ui.actions;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackage().getName()+".messages"; //$NON-NLS-1$
	public static String AboutHandler_BuildTimeInfoFormat;
	public static String AboutHandler_InfoTemplate;
	public static String AboutHandler_Title;
	public static String CreateCustomizationProjectHandler_DefaultName;
	public static String CreateCustomizationProjectHandler_NamePromptDesc;
	public static String CreateCustomizationProjectHandler_ProvideName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
