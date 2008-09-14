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

public class ImportClonkFiles extends Wizard implements IImportWizard {
    private IWorkbench workbench;

    private IStructuredSelection selection;

    private ImportClonkPage1 mainPage;	
	
    public ImportClonkFiles() {
        IDialogSettings workbenchSettings = WorkbenchPlugin.getDefault().getDialogSettings();
        IDialogSettings section = workbenchSettings
                .getSection("FileSystemImportWizard");//$NON-NLS-1$
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
        
//        List selectedResources = IDE.computeSelectedResources(currentSelection);
//        if (!selectedResources.isEmpty()) {
//            this.selection = new StructuredSelection(selectedResources);
//        }

        setWindowTitle("Import from Clonk directory to project");
//        setDefaultPageImageDescriptor(IDEWorkbenchPlugin.getIDEImageDescriptor("wizban/importdir_wiz.png"));//$NON-NLS-1$
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
    	ClonkImportOperation op = new ClonkImportOperation(mainPage.getFilesToImport().toArray(new File[] {}),container);
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
//    	boolean importSuccess = true;
//        if (importSuccess) {
//        	try {
//        		IPath projectPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(mainPage.getDestinationPath());
//            	List<Object> files = mainPage.getFilesToImport();
//            	for(Object e : files) {
//            		if (e == null) continue;
//            		if (e instanceof File) {
//            			File file = (File)e;
////            			File file = projectPath.append(((File) e).getName()).toFile();
//        				C4Group group = C4Group.OpenFile(file);
//        				group.open(true);
//        				
////        				IFolder parent = ResourcesPlugin.getWorkspace().getRoot().getFolder(projectPath);
////        				IPath flub = parent.getLocation();
//        				group.extractToFilesystem(ResourcesPlugin.getWorkspace().getRoot().getProject("test"));
//        				group.close();
//        				group = null; // destruct
////        				file.delete();
//            		}
//            	}
//            	ResourcesPlugin.getWorkspace().getRoot().getFile(projectPath).refreshLocal(IFile.DEPTH_INFINITE, null);
//            	
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//				return false;
//			} catch (InvalidDataException e) {
//				e.printStackTrace();
//				return false;
//			} catch (CoreException e) {
//				e.printStackTrace();
//				return false;
//			}
//        }
//        return importSuccess;
    }


}
