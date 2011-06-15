package net.arctics.clonk.ui.wizards;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.preferences.C4GroupListEditor;
import net.arctics.clonk.preferences.ClonkPreferencePage;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.preferences.IEngineProvider;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.UI;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class WizardNewClonkProjectCreationPage extends WizardNewProjectCreationPage implements IEngineProvider {

	private C4GroupListEditor linkGroupsEditor, importGroupsEditor;
	private CheckboxTableViewer projectReferencesViewer;
	private ComboFieldEditor engineEditor;
	private PreferenceStore dummyPrefStore;
	private TabItem linkTab, importTab, referenceTab;
	
	public WizardNewClonkProjectCreationPage(String pageName) {
		super(pageName);
		setImageDescriptor(UI.imageDescriptorForPath("icons/project_big.png"));
	}
	
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		
		Composite realParent = (Composite) getControl(); // surprise, that actually seems to work
		realParent.setLayout(new GridLayout(1, false));
		realParent.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		
		dummyPrefStore = new PreferenceStore();
		dummyPrefStore.setValue(ClonkPreferences.ACTIVE_ENGINE, ""); //$NON-NLS-1$
		
		Group engineGroup = new Group(realParent, SWT.SHADOW_IN);
		engineGroup.setText(Messages.NewClonkProject_Engine);
		engineGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		engineEditor = new ComboFieldEditor(ClonkPreferences.ACTIVE_ENGINE, net.arctics.clonk.preferences.Messages.ClonkPreferencePage_DefaultEngine, ClonkPreferencePage.engineComboValues(true), engineGroup);
		engineEditor.setPreferenceStore(dummyPrefStore);
		engineEditor.load();
		((GridData)engineGroup.getChildren()[1].getLayoutData()).grabExcessHorizontalSpace = true;
		engineGroup.setLayout(new GridLayout(2, true));
		
		TabFolder tabFolder = new TabFolder(realParent, SWT.DEFAULT);
		tabFolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		importTab = new TabItem(tabFolder, SWT.DEFAULT);
		importTab.setText(Messages.NewClonkProject_ImportingTabTitle);
		referenceTab = new TabItem(tabFolder, SWT.DEFAULT);
		referenceTab.setText(Messages.NewClonkProject_ProjectRefsTabTitle);
		linkTab = new TabItem(tabFolder, SWT.DEFAULT);
		linkTab.setText(Messages.NewClonkProject_LinkingTabTitle);
		
		Composite linksComposite = new Composite(tabFolder, SWT.NONE);
		linkGroupsEditor = new C4GroupListEditor("dummy", Messages.NewClonkProject_LinkGroups, linksComposite, this); //$NON-NLS-1$
		linkGroupsEditor.setPreferenceStore(dummyPrefStore);
		linkTab.setControl(linksComposite);
		
		Composite importsComposite = new Composite(tabFolder, SWT.NONE);
		importGroupsEditor = new C4GroupListEditor("dummy", Messages.NewClonkProject_ImportGroups, importsComposite, this); //$NON-NLS-1$
		importGroupsEditor.setPreferenceStore(dummyPrefStore);
		importTab.setControl(importsComposite);
	
		Composite referencesComposite = new Composite(tabFolder, SWT.NONE);
		referencesComposite.setLayout(new FillLayout());
		projectReferencesViewer = UI.createProjectReferencesViewer(referencesComposite);
		referenceTab.setControl(referencesComposite);
		
		parent.layout();
	}
	
	public String[] getGroupsToBeLinked() {
		return linkGroupsEditor.getValues();
	}
	
	public String[] getGroupsToBeImported() {
		return importGroupsEditor.getValues();
	}

	public IProject[] getProjectsToReference() {
		return ArrayUtil.convertArray(projectReferencesViewer.getCheckedElements(), IProject.class);
	}
	
	@Override
	public Engine getEngine(boolean fallbackToDefault) {
		engineEditor.store();
		Engine engine = ClonkCore.getDefault().loadEngine(dummyPrefStore.getString(ClonkPreferences.ACTIVE_ENGINE));
		if (fallbackToDefault && engine == null)
			engine = ClonkCore.getDefault().getActiveEngine();
		return engine;
	}
	
}