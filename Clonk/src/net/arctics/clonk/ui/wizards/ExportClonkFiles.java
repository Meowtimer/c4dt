package net.arctics.clonk.ui.wizards;

import java.io.File;
import java.io.FileNotFoundException;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.InvalidDataException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardExportResourcesPage;

public class ExportClonkFiles extends Wizard implements IExportWizard {

    private IStructuredSelection selection;
	
    private WizardExportResourcesPage exportPage;
    
	public ExportClonkFiles() {

	}
	
	@Override
	public boolean performFinish() {
		if (selection.getFirstElement() instanceof IFolder) {
			IFolder folder = (IFolder) selection.getFirstElement();
			IEclipsePreferences prefs = new ProjectScope(folder.getProject()).getNode(ClonkCore.PLUGIN_ID);
			String path = prefs.get("clonkpath",null);
			try {
				C4Group.createFile(folder, new File(path));
				return true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			} catch (InvalidDataException e) {
				e.printStackTrace();
				return false;
			}
		}
		else {
			return false;
		}
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.selection = selection;
		exportPage = new ClonkExportResourcePage("Export to Clonk directory", selection);
		exportPage.setWizard(this);
		exportPage.setMessage("Choose folders to export and check output directory.");
		exportPage.setTitle("Title");
		exportPage.setMessage("blub", WizardPage.INFORMATION);
		setWindowTitle("Export to Clonk directory");
		addPage(exportPage);
	}

}
