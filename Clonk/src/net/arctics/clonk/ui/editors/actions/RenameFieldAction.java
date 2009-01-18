package net.arctics.clonk.ui.editors.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;

import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.refactoring.ClonkRenameFieldProcessor;
import net.arctics.clonk.ui.refactoring.ClonkRenameRefactoringWizard;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class RenameFieldAction extends OpenDeclarationAction {

	public RenameFieldAction(ResourceBundle bundle, String prefix,
			ITextEditor editor) {
		super(bundle, prefix, editor);
	}

	@Override
	public void run() {
		try {
			C4Field fieldToRename = getFieldAtSelection();
			if (fieldToRename != null) {
				RenameRefactoring refactoring = new RenameRefactoring(new ClonkRenameFieldProcessor(fieldToRename, "newName")) {
					@Override
					public Change createChange(IProgressMonitor pm)
							throws CoreException {
						return super.createChange(pm);
					}
				};
				ClonkRenameRefactoringWizard wizard = new ClonkRenameRefactoringWizard(refactoring);
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.create();
				dialog.open();
			}
		}
		catch (ClassCastException cce) {
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
