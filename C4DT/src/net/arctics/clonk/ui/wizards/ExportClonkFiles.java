package net.arctics.clonk.ui.wizards;

import net.arctics.clonk.resource.c4group.C4GroupExporter;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
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
		IResource[] selectedResources = exportPage.getSelectedResources();
		IContainer[] selectedContainers = new IContainer[selectedResources.length];
		for(int i = 0; i < selectedResources.length;i++) {
			if (selectedResources[i] instanceof IContainer)
				selectedContainers[i] = (IContainer) selectedResources[i];
		}
		C4GroupExporter exporter = new C4GroupExporter(selectedContainers, null);
		if (exporter.selectDestPaths())
			exporter.export(null);
		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		exportPage = new ExportResourcesPage(Messages.ExportClonkFiles_ExportToClonkDir);
		exportPage.setWizard(this);
		exportPage.setMessage(Messages.ExportClonkFiles_ChooseFoldersToExport);
		exportPage.setTitle(Messages.ExportClonkFiles_Title);
		exportPage.setMessage(Messages.ExportClonkFiles_Information, WizardPage.INFORMATION);
		setWindowTitle(Messages.ExportClonkFiles_ExportToClonkDir);
		addPage(exportPage);
	}

}
