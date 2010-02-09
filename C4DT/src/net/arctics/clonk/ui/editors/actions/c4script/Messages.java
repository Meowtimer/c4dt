package net.arctics.clonk.ui.editors.actions.c4script;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.ui.editors.actions.c4script.messages"; //$NON-NLS-1$
	public static String TidyUpCodeAction_TidyUpCode;
	public static String DeclarationChooser_Label;
	public static String FindReferencesAction_Label;
	public static String FindReferencesAction_OnlyWorksWithinProject;
	public static String RenameDeclarationAction_Failed;
	public static String RenameDeclarationAction_RenamingFailed;
	public static String RenameDeclarationAction_RenameDeclaration;
	public static String RenameDeclarationAction_SpecifyNewName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
