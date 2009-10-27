package net.arctics.clonk.ui.refactoring;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "net.arctics.clonk.ui.refactoring.messages"; //$NON-NLS-1$
	public static String ClonkRenameInputWizardPage_SupplyTheName;
	public static String ClonkRenameInputWizardPage_SupplyTheNameDesc;
	public static String ClonkRenameInputWizardPage_NewName;
	public static String ClonkRenameRefactoringWizard_NewName;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
