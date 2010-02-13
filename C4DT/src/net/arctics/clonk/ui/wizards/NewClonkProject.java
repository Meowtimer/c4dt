package net.arctics.clonk.ui.wizards;

import java.io.File;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.preferences.C4GroupListEditor;
import net.arctics.clonk.ui.navigator.LinkC4GroupFileHandler;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class NewClonkProject extends Wizard implements INewWizard {
	
	private class WizardNewClonkProjectCreationPage extends WizardNewProjectCreationPage {

		private C4GroupListEditor linkGroupsEditor;
		
		private static final String groupsToBeLinkedPref = "groupsToBeLinked"; //$NON-NLS-1$
		
		public WizardNewClonkProjectCreationPage(String pageName) {
			super(pageName);
		}
		
		@Override
		public void createControl(Composite parent) {
			super.createControl(parent);
			Composite composite = new Composite((Composite) parent.getChildren()[0], SWT.NULL);
			composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			linkGroupsEditor = new C4GroupListEditor(groupsToBeLinkedPref, Messages.NewClonkProject_LinkedGroups, composite);
			linkGroupsEditor.setPreferenceStore(new PreferenceStore());
		}
		
		public String[] getGroupsToBeLinked() {
			return linkGroupsEditor.getValues();
		}
		
	}

	protected WizardNewClonkProjectCreationPage page;
	
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
			proj.create(desc,null);
			proj.open(null);
			for (String group : page.getGroupsToBeLinked()) {
				LinkC4GroupFileHandler.linkC4GroupFile(proj, new File(group));
			}
			return true;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		page = new WizardNewClonkProjectCreationPage("newProject"); //$NON-NLS-1$
		page.setTitle(Messages.NewClonkProject_PageTitle);
		addPage(page);
		setWindowTitle(Messages.NewClonkProject_WindowTitle);
	}

}
