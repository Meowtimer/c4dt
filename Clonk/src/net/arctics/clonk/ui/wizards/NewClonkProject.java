package net.arctics.clonk.ui.wizards;

import org.eclipse.core.internal.resources.ProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class NewClonkProject extends Wizard implements INewWizard {

	protected WizardNewProjectCreationPage page;
	
	public boolean performFinish() {
		IProject proj = page.getProjectHandle();
		
		IProjectDescription desc = new ProjectDescription();
		try {
			desc.setNatureIds(new String[] { "net.arctics.clonk.clonknature" });
			proj.create(null);
			return true;
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		page = new WizardNewProjectCreationPage("newProject");
		addPage(page);
	}

}
