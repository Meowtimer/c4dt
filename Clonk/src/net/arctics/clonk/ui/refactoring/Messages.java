package net.arctics.clonk.ui.refactoring;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.ui.refactoring.messages"; //$NON-NLS-1$
	public static String ClonkRenameInputWizardPage_0;
	public static String ClonkRenameInputWizardPage_1;
	public static String ClonkRenameInputWizardPage_2;
	public static String ClonkRenameRefactoringWizard_0;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
