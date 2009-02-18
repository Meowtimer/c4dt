package net.arctics.clonk.ui.editors.actions;

import java.util.ResourceBundle;

import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.refactoring.ClonkRenameFieldProcessor;
import net.arctics.clonk.ui.editors.ClonkCommandIds;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ui.texteditor.ITextEditor;

public class RenameFieldAction extends OpenDeclarationAction {

	public RenameFieldAction(ResourceBundle bundle, String prefix,
			ITextEditor editor) {
		super(bundle, prefix, editor);
		this.setActionDefinitionId(ClonkCommandIds.RENAME_FIELD);
	}

	@Override
	public void run() {
		try {
			C4Field fieldToRename = getFieldAtSelection();
			if (fieldToRename != null) {
				InputDialog newNameDialog = new InputDialog(getTextEditor().getSite().getWorkbenchWindow().getShell(), "Name Of Field", "Specify the new name here", fieldToRename.getName(), null);
				switch (newNameDialog.open()) {
				case InputDialog.CANCEL:
					return;
				}
				String newName = newNameDialog.getValue();
				RenameRefactoring refactoring = new RenameRefactoring(new ClonkRenameFieldProcessor(fieldToRename, newName));
				PerformChangeOperation op = new PerformChangeOperation(
					new CreateChangeOperation(
						new CheckConditionsOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS), RefactoringStatus.FATAL
					)
				);
				op.run(null);
//				ClonkRenameRefactoringWizard wizard = new ClonkRenameRefactoringWizard(refactoring);
//				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
//				WizardDialog dialog = new WizardDialog(shell, wizard);
//				dialog.create();
//				dialog.open();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
