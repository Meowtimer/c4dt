package net.arctics.clonk.ui.wizards;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.preferences.PreferenceConstants;
import net.arctics.clonk.resource.c4group.C4GroupExporter;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

public class ExportClonkFiles extends Wizard implements IExportWizard {
	
    private ExportResourcesPage exportPage;
    
	public ExportClonkFiles() {

	}
	
	public boolean performFinish() {
		IPreferencesService service = Platform.getPreferencesService();
		String c4groupPath = service.getString(ClonkCore.PLUGIN_ID, PreferenceConstants.C4GROUP_EXECUTABLE, "", null);
		String gamePath = service.getString(ClonkCore.PLUGIN_ID, PreferenceConstants.GAME_PATH, null, null);
		IResource[] selectedResources = exportPage.getSelectedResources();
		IContainer[] selectedContainers = new IContainer[selectedResources.length];
		for(int i = 0; i < selectedResources.length;i++) {
			if (selectedResources[i] instanceof IContainer)
				selectedContainers[i] = (IContainer) selectedResources[i];
		}
		C4GroupExporter exporter = new C4GroupExporter(selectedContainers,c4groupPath,gamePath);
		exporter.export(null);
		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
//        this.selection = selection;
		
		exportPage = new ExportResourcesPage("Export to Clonk directory");
		exportPage.setWizard(this);
		exportPage.setMessage("Choose folders to export and check output directory.");
		exportPage.setTitle("C4Group Export");
		exportPage.setMessage("blub", WizardPage.INFORMATION);
		setWindowTitle("Export to Clonk directory");
		addPage(exportPage);
	}

}
