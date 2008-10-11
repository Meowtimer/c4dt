package net.arctics.clonk.ui.wizards;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.FileSystemElement;
import org.eclipse.ui.internal.wizards.datatransfer.WizardFileSystemResourceImportPage1;

@SuppressWarnings("restriction")
public class ImportClonkPage1 extends WizardFileSystemResourceImportPage1 {
	

    /**
     *	Creates an instance of this class
     *
     * @param aWorkbench IWorkbench
     * @param selection IStructuredSelection
     */
    public ImportClonkPage1(IWorkbench aWorkbench,
            IStructuredSelection selection) {
        super(aWorkbench, selection);
    }
    
    public IPath getDestinationPath() {
    	return getResourcePath();
    }
    
    @SuppressWarnings("unchecked")
	public List<Object> getFilesToImport() {
        Iterator resourcesEnum = getSelectedResources().iterator();
        List<Object> fileSystemObjects = new ArrayList<Object>();
        while (resourcesEnum.hasNext()) {
            fileSystemObjects.add(((FileSystemElement) resourcesEnum.next())
                    .getFileSystemObject());
        }

        return fileSystemObjects;
    }

}
