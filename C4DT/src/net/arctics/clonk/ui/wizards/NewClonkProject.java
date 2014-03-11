package net.arctics.clonk.ui.wizards;

import java.io.File;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.ui.navigator.LinkC4GroupFileHandler;
import net.arctics.clonk.ui.navigator.QuickImportHandler;
import net.arctics.clonk.util.ArrayUtil;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class NewClonkProject extends Wizard implements INewWizard {

	protected NewClonkProjectWizardCreationPage page;

	@Override
	public boolean performFinish() {
		try {
			final IProject proj = page.getProjectHandle();
			final IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(page.getProjectName());
			if (!page.useDefaults())
				desc.setLocation(page.getLocationPath());
			desc.setNatureIds(new String[] {Core.NATURE_ID});
			final ICommand command = desc.newCommand();
			command.setBuilderName(Core.id("builder")); //$NON-NLS-1$
			desc.setBuildSpec(new ICommand[] {command});
			desc.setReferencedProjects(page.getProjectsToReference());
			proj.create(desc,null);
			proj.open(null);

			final ClonkProjectNature nature = ClonkProjectNature.get(proj);
			nature.settings().setEngineName(page.getEngine(false) != null ? page.getEngine(false).name() : "");
			nature.saveSettings();


			// link and import
			for (final String group : page.getGroupsToBeLinked())
				LinkC4GroupFileHandler.linkC4GroupFile(proj, new File(group));
			QuickImportHandler.importFiles(getShell(), proj, ArrayUtil.map(page.getGroupsToBeImported(), File.class, from -> new File(from)));

			return true;
		} catch (final CoreException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void init(final IWorkbench workbench, final IStructuredSelection selection) {
		page = new NewClonkProjectWizardCreationPage("newProject"); //$NON-NLS-1$
		page.setTitle(Messages.NewClonkProject_PageTitle);
		addPage(page);
		setWindowTitle(Messages.NewClonkProject_WindowTitle);
	}

}
