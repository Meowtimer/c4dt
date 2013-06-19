package net.arctics.clonk.c4script.typing.dabble;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackage().getName()+".messages"; //$NON-NLS-1$
	public static String Apply;
	public static String ComputingGraph;
	public static String RunInference;
	public static String UnknownProject;
	public static String ValidateParameters;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
