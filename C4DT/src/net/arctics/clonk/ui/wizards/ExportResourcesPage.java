package net.arctics.clonk.ui.wizards;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.ResourceContentProvider;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ExportResourcesPage extends WizardPage {

	private CheckboxTreeViewer folderSelector;
	
	protected ExportResourcesPage(String pageName) {
		super(pageName);
	}

	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.FILL);
		setTitle(Messages.ExportResourcesPage_Title);
		setMessage(Messages.ExportResourcesPage_Desc);
		GridLayout layout = new GridLayout();
		comp.setLayout(layout);
		layout.numColumns = 1;
		layout.verticalSpacing = 9;
		
		createResourcesGroup(comp);
		createDestinationGroup(comp);
		
		// get current selection
		if (PlatformUI.getWorkbench() == null ||
				PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ||
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService() == null ||
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection() == null)
			return;
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (selection != null && selection instanceof TreeSelection) {
			TreeSelection tree = (TreeSelection) selection;
			for(Object obj : tree.toList()) {
				if (obj instanceof IFolder) {
					folderSelector.expandToLevel(obj, 0);
					// select selected item
					folderSelector.setChecked(obj, true);
				}
			}
			
		}
		
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
		String gamePath = null;
		for (IResource res : getSelectedResources()) {
			Engine engine = ClonkProjectNature.getEngine(res);
			if (engine != null) {
				gamePath = engine.getCurrentSettings().gamePath;
				break;
			}
		}
		if (gamePath != null) {
			lab.setText(String.format(Messages.ExportResourcesPage_OutputDir, gamePath));
		}
		else {
			lab.setText(Messages.ExportResourcesPage_GamePathMissing);
		}
	}

}
