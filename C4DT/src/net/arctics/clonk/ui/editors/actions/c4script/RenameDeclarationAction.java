package net.arctics.clonk.ui.editors.actions.c4script;

import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.refactoring.ClonkRenameFieldProcessor;
import net.arctics.clonk.ui.editors.EditorUtil;
import net.arctics.clonk.ui.editors.IClonkCommandIds;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class RenameDeclarationAction extends OpenDeclarationAction {

	public RenameDeclarationAction(ResourceBundle bundle, String prefix,
			ITextEditor editor) {
		super(bundle, prefix, editor);
		this.setActionDefinitionId(IClonkCommandIds.RENAME_DECLARATION);
	}
	
	private boolean displayRefactoringError(RefactoringStatus status) {
		if (status == null)
			return false;
		RefactoringStatusEntry entry = status.getEntryWithHighestSeverity();
		if (entry != null && entry.getSeverity() == RefactoringStatus.FATAL) {
			ErrorDialog.openError(null, Messages.RenameDeclarationAction_Failed, Messages.RenameDeclarationAction_RenamingFailed, new Status(IStatus.ERROR, ClonkCore.PLUGIN_ID, entry.getMessage()));
			return true;
		}
		return false;
	}

	@Override
	public void run() {
		
		saveModifiedFiles();
		
		try {
			C4Declaration fieldToRename = getDeclarationAtSelection();
			if (fieldToRename != null) {
				InputDialog newNameDialog = new InputDialog(getTextEditor().getSite().getWorkbenchWindow().getShell(), Messages.RenameDeclarationAction_RenameDeclaration, Messages.RenameDeclarationAction_SpecifyNewName, fieldToRename.getName(), null);
				switch (newNameDialog.open()) {
				case InputDialog.CANCEL:
					return;
				}
				String newName = newNameDialog.getValue();
				RenameRefactoring refactoring = new RenameRefactoring(new ClonkRenameFieldProcessor(fieldToRename, newName));
				CheckConditionsOperation checkConditions = new CheckConditionsOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
				CreateChangeOperation createChange = new CreateChangeOperation(checkConditions, RefactoringStatus.FATAL);
				PerformChangeOperation op = new PerformChangeOperation(createChange);
				op.run(null);
				
				if (!displayRefactoringError(op.getConditionCheckingStatus()))
					displayRefactoringError(op.getValidationStatus());
				
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
	
	private void saveModifiedFiles() {

		boolean anyModified = EditorUtil.editorPartsToBeSaved().iterator().hasNext();

		if (anyModified) {
			final ProgressMonitorDialog progressMonitor = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
			try {
				progressMonitor.run(false, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						for (IEditorPart part : EditorUtil.editorPartsToBeSaved()) {
							part.doSave(monitor);
						}
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
