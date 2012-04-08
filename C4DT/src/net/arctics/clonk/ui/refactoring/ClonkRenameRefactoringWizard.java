package net.arctics.clonk.ui.refactoring;

import net.arctics.clonk.refactoring.RenameDeclarationProcessor;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class ClonkRenameRefactoringWizard extends RefactoringWizard {
	
	private boolean letUserSpecifyNewName;

	public ClonkRenameRefactoringWizard(Refactoring refactoring, boolean letUserSpecifyNewName) {
		super(refactoring, RefactoringWizard.WIZARD_BASED_USER_INTERFACE);
		this.letUserSpecifyNewName = letUserSpecifyNewName;
	}

	@Override
	protected void addUserInputPages() {
		if (letUserSpecifyNewName)
			addPage(new ClonkRenameInputWizardPage(Messages.ClonkRenameRefactoringWizard_NewName,
					(RenameDeclarationProcessor) getRefactoring().getAdapter(RenameDeclarationProcessor.class)));
	}

}
