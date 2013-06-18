package net.arctics.clonk.c4script.ast;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackage().getName()+".messages"; //$NON-NLS-1$
	public static String AccessDeclaration_TypesSporting;
	public static String DabbleInference_9;
	public static String DabbleInference_Apply;
	public static String DabbleInference_ComputingGraph;
	public static String DabbleInference_RunInference;
	public static String DabbleInference_UnknownProject;
	public static String DabbleInference_ValidateParameters;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
