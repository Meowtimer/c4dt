package net.arctics.clonk.ui.wizards;

import net.arctics.clonk.ClonkCore;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class NewClonkProject extends Wizard implements INewWizard {

	protected WizardNewProjectCreationPage page;
	
	public boolean performFinish() {
		try {
			IProject proj = page.getProjectHandle();
			IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(page.getProjectName());
			if (!page.useDefaults())
				desc.setLocation(page.getLocationPath());
			desc.setNatureIds(new String[] { ClonkCore.id("clonknature") });
			ICommand command = desc.newCommand();
			command.setBuilderName(ClonkCore.id("builder"));
			desc.setBuildSpec(new ICommand[] { command });
			proj.create(desc,null);
			proj.open(null);
			return true;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		page = new WizardNewProjectCreationPage("newProject");
		page.setTitle("Create a Clonk Project");
		addPage(page);
		setWindowTitle("New Clonk Project");
	}

}
