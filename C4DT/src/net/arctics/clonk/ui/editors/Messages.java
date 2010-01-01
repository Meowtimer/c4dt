package net.arctics.clonk.ui.editors;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.ui.editors.messages"; //$NON-NLS-1$
	public static String ClonkColorConstants_BRACKET;
	public static String ClonkColorConstants_COMMENT;
	public static String ClonkColorConstants_DEFAULT;
	public static String ClonkColorConstants_DIRECTIVE;
	public static String ClonkColorConstants_ENGINE_FUNCTION;
	public static String ClonkColorConstants_KEYWORD;
	public static String ClonkColorConstants_NUMBER;
	public static String ClonkColorConstants_OBJ_CALLBACK;
	public static String ClonkColorConstants_OPERATOR;
	public static String ClonkColorConstants_PROC_INSTR;
	public static String ClonkColorConstants_RETURN;
	public static String ClonkColorConstants_STRING;
	public static String ClonkColorConstants_TYPE;
	public static String ClonkHyperlink_Label;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
