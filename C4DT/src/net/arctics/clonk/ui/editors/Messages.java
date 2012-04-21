package net.arctics.clonk.ui.editors;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.ui.editors.messages"; //$NON-NLS-1$
	public static String ColorManager_BRACKET;
	public static String ColorManager_COMMENT;
	public static String ColorManager_DEFAULT;
	public static String ColorManager_DIRECTIVE;
	public static String ColorManager_ENGINE_FUNCTION;
	public static String ColorManager_JAVADOCCOMMENT;
	public static String ColorManager_KEYWORD;
	public static String ColorManager_NUMBER;
	public static String ColorManager_OBJ_CALLBACK;
	public static String ColorManager_OPERATOR;
	public static String ColorManager_RETURN;
	public static String ColorManager_STRING;
	public static String ColorManager_TYPE;
	public static String ClonkHyperlink_ChooseLinkTargetTitle;
	public static String ClonkHyperlink_Label;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
