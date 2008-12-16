package net.arctics.clonk.ui.wizards;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.preferences.PreferenceConstants;
import net.arctics.clonk.resource.ResourceContentProvider;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ExportResourcesPage extends WizardPage {

	private CheckboxTreeViewer folderSelector;
	
	protected ExportResourcesPage(String pageName) {
		super(pageName);
	}

	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.FILL);
		setTitle("Export folders");
		setMessage("Select folders which should be exported as c4group files");
		GridLayout layout = new GridLayout();
		comp.setLayout(layout);
		layout.numColumns = 1;
		layout.verticalSpacing = 9;
		
		createResourcesGroup(comp);
		createDestinationGroup(comp);
		
		setControl(comp);
	}
	
	public IResource[] getSelectedResources() {
		IResource[] ress = new IResource[folderSelector.getCheckedElements().length];
		Object[] obj = folderSelector.getCheckedElements();
		for(int i = 0; i < obj.length;i++) {
			ress[i] = (IResource) obj[i];
		}
		return ress;
	}
	
	protected void createResourcesGroup(Composite parent) {
        Tree tree = new Tree(parent, SWT.CHECK | SWT.BORDER);
        GridData data = new GridData(GridData.FILL_BOTH);
//        if (useHeightHint) {
//			data.heightHint = PREFERRED_HEIGHT;
//		}
        tree.setLayoutData(data);
        tree.setFont(parent.getFont());
		folderSelector = new CheckboxTreeViewer(tree);
		folderSelector.setContentProvider(new ResourceContentProvider(ResourcesPlugin.getWorkspace().getRoot(),IResource.FOLDER | IResource.PROJECT));
		folderSelector.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
		folderSelector.setInput(ResourcesPlugin.getWorkspace().getRoot());
	}
	
	protected void createDestinationGroup(Composite parent) {
		Label lab= new Label(parent,SWT.NONE);
		IPreferencesService service = Platform.getPreferencesService();
		String gamePath = service.getString(ClonkCore.PLUGIN_ID, PreferenceConstants.GAME_PATH, "Not set. Set Clonk path in Project properties.", null);
		if (gamePath != null) {
			lab.setText("Output dir: " + gamePath);
		}
		else {
			lab.setText("Configure your Clonk game path to export.");
		}
	}

}
