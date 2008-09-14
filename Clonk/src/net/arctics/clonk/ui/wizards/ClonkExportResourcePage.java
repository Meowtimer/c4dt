package net.arctics.clonk.ui.wizards;

import java.util.List;

import net.arctics.clonk.ClonkCore;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.WizardExportResourcesPage;

public class ClonkExportResourcePage extends WizardExportResourcesPage {

	protected IStructuredSelection selection;
	
	public ClonkExportResourcePage(String pageName,
			IStructuredSelection selection) {
		super(pageName, selection);
		this.selection = selection;
	}

	public void handleEvent(Event event) {
		System.out.print("event");
	}


	public List<IResource> getResources() {
		return getSelectedResources();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardDataTransferPage#createOptionsGroup(org.eclipse.swt.widgets.Composite)
	 */
	protected void createOptionsGroup(Composite parent) {
		// TODO Auto-generated method stub
		//super.createOptionsGroup(parent);
		
	}

	protected void createDestinationGroup(Composite parent) {
		Label lab= new Label(parent,SWT.NONE);
		if (selection.getFirstElement() instanceof IFolder) {
			IFolder folder = (IFolder)selection.getFirstElement();
			IEclipsePreferences prefs = new ProjectScope(folder.getProject()).getNode(ClonkCore.PLUGIN_ID);
			if (prefs != null) {
				String path = prefs.get("clonkpath", "Not set. Set Clonk path in Project properties.");
				lab.setText("Output dir: " + path);
			}
		}

	}

}
