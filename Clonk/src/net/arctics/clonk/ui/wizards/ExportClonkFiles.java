package net.arctics.clonk.ui.wizards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.preferences.PreferenceConstants;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.InvalidDataException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
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
	
	public boolean performFinish() {
		if (selection.getFirstElement() instanceof IFolder) {
			IFolder folder = (IFolder) selection.getFirstElement();
			IEclipsePreferences prefs = new ProjectScope(folder.getProject()).getNode(ClonkCore.PLUGIN_ID);
			IPreferencesService service = Platform.getPreferencesService();
			String c4groupPath = service.getString(ClonkCore.PLUGIN_ID, PreferenceConstants.C4GROUP_EXECUTABLE, "", null);
			String path = prefs.get("clonkpath",null);
			try {
				String cmd = c4groupPath + " " + new Path(path).append(folder.getName()).toOSString() + " /r -a " + new Path(folder.getLocation().toString()).append("*").toOSString();
				System.out.println(cmd);
				Process c4group = Runtime.getRuntime().exec(cmd);
				int status = c4group.waitFor();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return true;
//			try {
//				C4Group.createFile(folder, new File(path));
//				return true;
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//				return false;
//			} catch (InvalidDataException e) {
//				e.printStackTrace();
//				return false;
//			}
		}
		else {
			return false;
		}
	}

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
