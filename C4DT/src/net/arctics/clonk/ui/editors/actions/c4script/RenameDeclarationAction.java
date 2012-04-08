package net.arctics.clonk.ui.editors.actions.c4script;

import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.IIndexEntity;
import net.arctics.clonk.refactoring.RenameDeclarationProcessor;
import net.arctics.clonk.ui.editors.ClonkCommandIds;
import net.arctics.clonk.ui.editors.EditorUtil;
import net.arctics.clonk.ui.refactoring.ClonkRenameRefactoringWizard;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class RenameDeclarationAction extends C4ScriptEditorAction {

	public RenameDeclarationAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor, ClonkCommandIds.RENAME_DECLARATION);
	}
	
	@Override
	public void run() {
		try {
			IIndexEntity entity = entityAtSelection(false);
			if (entity != null)
				performRenameRefactoring((Declaration)entity, null, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Perform a Rename Refactoring on the specified declaration. Custom options are passed to {@link RenameDeclarationProcessor#RenameDeclarationProcessor(Declaration, String, int)}
	 * @param declarationToRename The {@link Declaration} to rename
	 * @param fixedNewName New name to perform this refactoring with, without presenting UI to change this name. Supply null to let the user specify the new name
	 * @param renameProcessorOptions {@link RenameDeclarationProcessor} options
	 */
	public static void performRenameRefactoring(Declaration declarationToRename, String fixedNewName, int renameProcessorOptions) {
		if (declarationToRename != null) {
			saveModifiedFiles();
			String newName = fixedNewName != null ? fixedNewName : declarationToRename.name();
			RenameRefactoring refactoring = new RenameRefactoring(new RenameDeclarationProcessor(declarationToRename, newName, renameProcessorOptions));
			ClonkRenameRefactoringWizard wizard = new ClonkRenameRefactoringWizard(refactoring, fixedNewName == null);
			RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
			try {
				op.run(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Performing Clonk Rename refactoring");
			} catch (InterruptedException e) {
				// do nothing
			}
		}
	}
	
	private static void saveModifiedFiles() {

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
