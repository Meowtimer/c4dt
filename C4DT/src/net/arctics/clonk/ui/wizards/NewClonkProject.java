package net.arctics.clonk.ui.wizards;

import java.io.File;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.preferences.C4GroupListEditor;
import net.arctics.clonk.preferences.ClonkPreferencePage;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.preferences.Messages;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.navigator.LinkC4GroupFileHandler;
import net.arctics.clonk.ui.navigator.QuickImportHandler;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class NewClonkProject extends Wizard implements INewWizard {
	
	private class WizardNewClonkProjectCreationPage extends WizardNewProjectCreationPage {

		private C4GroupListEditor linkGroupsEditor, importGroupsEditor;
		private CheckboxTableViewer projectReferencesViewer;
		private ComboFieldEditor engineEditor;
		private PreferenceStore dummyPrefStore;
		private TabItem linkTab, importTab, referenceTab;
		
		public WizardNewClonkProjectCreationPage(String pageName) {
			super(pageName);
		}
		
		@Override
		public void createControl(Composite parent) {
			super.createControl(parent);
			
			Composite realParent = (Composite) parent.getChildren()[0];
			realParent.setLayoutData(new GridData(GridData.FILL_BOTH));
			dummyPrefStore = new PreferenceStore();
			dummyPrefStore.setValue(ClonkPreferences.ACTIVE_ENGINE, ""); //$NON-NLS-1$
			
			Group engineGroup = new Group(realParent, SWT.SHADOW_IN);
			engineGroup.setText(Messages.NewClonkProject_Engine);
			engineGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			engineEditor = new ComboFieldEditor(ClonkPreferences.ACTIVE_ENGINE, net.arctics.clonk.preferences.Messages.EngineVersion, ClonkPreferencePage.engineComboValues(true), engineGroup);
			engineEditor.setPreferenceStore(dummyPrefStore);
			engineEditor.load();
			((GridData)engineGroup.getChildren()[1].getLayoutData()).grabExcessHorizontalSpace = true;
			engineGroup.setLayout(new GridLayout(2, true));
			
			TabFolder tabFolder = new TabFolder(realParent, SWT.DEFAULT);
			tabFolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			linkTab = new TabItem(tabFolder, SWT.DEFAULT);
			linkTab.setText(Messages.NewClonkProject_LinkingTabTitle);
			importTab = new TabItem(tabFolder, SWT.DEFAULT);
			importTab.setText(Messages.NewClonkProject_ImportingTabTitle);
			referenceTab = new TabItem(tabFolder, SWT.DEFAULT);
			referenceTab.setText(Messages.NewClonkProject_ProjectRefsTabTitle);
			
			Composite linksComposite = new Composite(tabFolder, SWT.NONE);
			linkGroupsEditor = new C4GroupListEditor("dummy", Messages.NewClonkProject_LinkGroups, linksComposite); //$NON-NLS-1$
			linkGroupsEditor.setPreferenceStore(dummyPrefStore);
			linkTab.setControl(linksComposite);
			
			Composite importsComposite = new Composite(tabFolder, SWT.NONE);
			importGroupsEditor = new C4GroupListEditor("dummy", Messages.NewClonkProject_ImportGroups, importsComposite); //$NON-NLS-1$
			importGroupsEditor.setPreferenceStore(dummyPrefStore);
			importTab.setControl(importsComposite);
		
			Composite referencesComposite = new Composite(tabFolder, SWT.NONE);
			referencesComposite.setLayout(new FillLayout());
			projectReferencesViewer = UI.createProjectReferencesViewer(referencesComposite);
			referenceTab.setControl(referencesComposite);
		}
		
		public String[] getGroupsToBeLinked() {
			return linkGroupsEditor.getValues();
		}
		
		public String[] getGroupsToBeImported() {
			return importGroupsEditor.getValues();
		}

		public IProject[] getProjectsToReference() {
			return Utilities.convertArray(projectReferencesViewer.getCheckedElements(), IProject.class);
		}
		
		public String getEngine() {
			engineEditor.store();
			return dummyPrefStore.getString(ClonkPreferences.ACTIVE_ENGINE);
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
			desc.setReferencedProjects(page.getProjectsToReference());
			proj.create(desc,null);
			proj.open(null);
			
			ClonkProjectNature.get(proj).getIndex().setEngineName(page.getEngine());
			
			// link and import
			for (String group : page.getGroupsToBeLinked()) {
				LinkC4GroupFileHandler.linkC4GroupFile(proj, new File(group));
			}
			QuickImportHandler.importFiles(getShell(), proj, Utilities.map(page.getGroupsToBeImported(), File.class, new IConverter<String, File>() {
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

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		page = new WizardNewClonkProjectCreationPage("newProject"); //$NON-NLS-1$
		page.setTitle(Messages.NewClonkProject_PageTitle);
		addPage(page);
		setWindowTitle(Messages.NewClonkProject_WindowTitle);
	}

}
