package net.arctics.clonk.ui.refactoring;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class ClonkRenameRefactoringWizard extends RefactoringWizard {

	public ClonkRenameRefactoringWizard(Refactoring refactoring) {
		super(refactoring, RefactoringWizard.WIZARD_BASED_USER_INTERFACE);
	}

	@Override
	protected void addUserInputPages() {
		addPage(new ClonkRenameInputWizardPage(Messages.ClonkRenameRefactoringWizard_0));
	}

}
