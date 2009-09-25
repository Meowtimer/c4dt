package net.arctics.clonk.ui.wizards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import net.arctics.clonk.resource.c4group.C4EntryHeader;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.resource.c4group.InvalidDataException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class ClonkImportOperation extends WorkspaceModifyOperation {

	private File[] resources;
	private IContainer destination;
	
	/**
	 * Use this constructor
	 * @param resourcesToImport
	 */
	public ClonkImportOperation(File[] resourcesToImport, IContainer destination) {
		super();
		resources = resourcesToImport;
		this.destination = destination;
	}

	@Override
	protected void execute(final IProgressMonitor monitor) throws CoreException,
	InvocationTargetException, InterruptedException {
		C4Group[] groups = new C4Group[resources.length];
		try {
			try {
				for(int i = 0; i < resources.length;i++) {
					groups[i] = C4Group.openFile(resources[i]);
					groups[i].readIntoMemory(true, new C4GroupItem.IHeaderFilter() {
						private IContainer currentContainer = destination;
						private C4Group currentGroup;
						
						public boolean accepts(C4EntryHeader header,
								C4Group context) {
							return true; // import whole group
						}

						public void processData(C4GroupItem item) throws CoreException {
							for (; currentGroup != item.getParentGroup(); currentGroup = currentGroup.getParentGroup(), currentContainer = currentContainer.getParent());
							if (item instanceof C4Group) {
								C4Group group = (C4Group)item;
								if (group.getParentGroup() == null)
									monitor.beginTask("Import " + group.getName(), group.getSizeOfChildren());
								IFolder newFolder = currentContainer.getFolder(new Path(group.getName()));
								newFolder.create(IResource.NONE, true, monitor);
								currentContainer = newFolder;
								currentGroup = group;
							}
							else {
								C4GroupEntry entry = (C4GroupEntry)item;
								IFile newFile = currentContainer.getFile(new Path(entry.getName()));
								newFile.create(entry.getContents(), IResource.NONE, monitor);
								entry.releaseData(); // release data to prevent heap overflow :S
							}
						}
					});
				}
			} catch (FileNotFoundException e) {
				monitor.setCanceled(true);
				throw new InvocationTargetException(e);
			} catch (InvalidDataException e) {
				monitor.setCanceled(true);
				throw new InvocationTargetException(e);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} finally {
			for (C4Group group : groups) {
				if (group != null)
					group.close();
			}
		}
		monitor.done();

	}

}
