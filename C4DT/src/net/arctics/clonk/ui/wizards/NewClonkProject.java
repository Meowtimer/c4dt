package net.arctics.clonk.ui.wizards;

import java.io.File;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.navigator.LinkC4GroupFileHandler;
import net.arctics.clonk.ui.navigator.QuickImportHandler;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;
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
	
	protected WizardNewClonkProjectCreationPage page;
	
	@Override
	public boolean performFinish() {
		try {
			IProject proj = page.getProjectHandle();
			IProjectDescription desc = ResourcesPlugin.getWorkspace().newProjectDescription(page.getProjectName());
			if (!page.useDefaults())
				desc.setLocation(page.getLocationPath());
			desc.setNatureIds(new String[] {ClonkCore.id("clonknature")}); //$NON-NLS-1$
			ICommand command = desc.newCommand();
			command.setBuilderName(ClonkCore.id("builder")); //$NON-NLS-1$
			desc.setBuildSpec(new ICommand[] {command});
			desc.setReferencedProjects(page.getProjectsToReference());
			proj.create(desc,null);
			proj.open(null);
			
			ClonkProjectNature.get(proj).getSettings().setEngineName(page.getEngine(false) != null ? page.getEngine(false).name() : "");
			
			// link and import
			for (String group : page.getGroupsToBeLinked()) {
				LinkC4GroupFileHandler.linkC4GroupFile(proj, new File(group));
			}
			QuickImportHandler.importFiles(getShell(), proj, ArrayUtil.map(page.getGroupsToBeImported(), File.class, new IConverter<String, File>() {
				@Override
				public File convert(String from) {
					return new File(from);
				}
			}));
			
			return true;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		page = new WizardNewClonkProjectCreationPage("newProject"); //$NON-NLS-1$
		page.setTitle(Messages.NewClonkProject_PageTitle);
		addPage(page);
		setWindowTitle(Messages.NewClonkProject_WindowTitle);
	}

}
