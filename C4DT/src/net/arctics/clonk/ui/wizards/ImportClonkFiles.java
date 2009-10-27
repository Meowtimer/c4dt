package net.arctics.clonk.ui.wizards;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.WorkbenchPlugin;

@SuppressWarnings("restriction")
public class ImportClonkFiles extends Wizard implements IImportWizard {
    private IWorkbench workbench;

    private IStructuredSelection selection;

    private ImportClonkPage1 mainPage;	
	
    public ImportClonkFiles() {
        IDialogSettings workbenchSettings = WorkbenchPlugin.getDefault().getDialogSettings();
        IDialogSettings section = workbenchSettings.getSection("FileSystemImportWizard");//$NON-NLS-1$
        if (section == null) {
			section = workbenchSettings.addNewSection("FileSystemImportWizard");//$NON-NLS-1$
		}
        setDialogSettings(section);
    }
	
    /* (non-Javadoc)
     * Method declared on IWizard.
     */
    public void addPages() {
        super.addPages();
        mainPage = new ImportClonkPage1(workbench, selection);
        addPage(mainPage);
    }


    /* (non-Javadoc)
     * Method declared on IWorkbenchWizard.
     */
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        this.workbench = workbench;
        this.selection = currentSelection;
        setWindowTitle(Messages.ImportClonkFiles_Title);
        setNeedsProgressMonitor(true);
    }

    /* (non-Javadoc)
     * Method declared on IWizard.
     */
    public boolean performFinish() {
    	IPath path = mainPage.getDestinationPath();
    	IContainer container = null;
    	if (path.segmentCount() == 1) {
    		container = ResourcesPlugin.getWorkspace().getRoot().getProject(path.toString());
    	}
    	else {
    		container = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
    	}
    	C4GroupImporter op = new C4GroupImporter(mainPage.getFilesToImport().toArray(new File[] {}),container);
    	try {
			getContainer().run(true, true, op);
			return true;
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
    }


}
