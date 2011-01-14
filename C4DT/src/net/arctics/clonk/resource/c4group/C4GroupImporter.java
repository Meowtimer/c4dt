package net.arctics.clonk.resource.c4group;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ui.wizards.Messages;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * Helper for importing c4group files as folders into a specified container
 *
 */
public class C4GroupImporter extends WorkspaceModifyOperation {

	private File[] resources;
	private IContainer destination;

	public C4GroupImporter(File[] resourcesToImport, IContainer destination) {
		super();
		resources = resourcesToImport;
		this.destination = destination;
	}
	
	@Override
	protected void execute(final IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		C4Group[] groups = new C4Group[resources.length];
		monitor.beginTask(Messages.C4GroupImporter_ImportingFiles, groups.length);
		try {
			for(int i = 0; i < resources.length;i++) {
				if (monitor.isCanceled())
					return;
				try {
					groups[i] = C4Group.openFile(resources[i]);
					monitor.subTask(String.format(Messages.C4GroupImporter_Importing, groups[i].getName()));
					final List<String> errorsWhileImporting = new LinkedList<String>();
					groups[i].readIntoMemory(true, new C4GroupHeaderFilterBase() {
						private IContainer currentContainer = destination;
						private C4Group currentGroup;

						public boolean accepts(C4GroupEntryHeader header, C4Group context) {
							return true; // import whole group
						}

						public void processData(C4GroupItem item) throws CoreException {
							for (; currentGroup != item.getParentGroup(); currentGroup = currentGroup.getParentGroup(), currentContainer = currentContainer.getParent());
							if (item instanceof C4Group) {
								C4Group group = (C4Group)item;
								/*if (group.getParentGroup() == null)
									monitor.subTask(String.format(Messages.C4GroupImporter_ImportTask, group.getName()));*/
								IFolder newFolder = currentContainer.getFolder(new Path(group.getName()));
								if (!newFolder.exists())
									newFolder.create(IResource.NONE, true, monitor);
								currentContainer = newFolder;
								currentGroup = group;
							}
							else {
								C4GroupEntry entry = (C4GroupEntry)item;
								IFile newFile = currentContainer.getFile(new Path(entry.getName()));
								InputStream newContents = entry.getContents();
								try {
									try {
										if (newFile.exists()) {
											newFile.setContents(newContents, 0, monitor);
										} else {
											newFile.create(entry.getContents(), IResource.NONE, monitor);
										}
									} catch (CoreException e) {
										errorsWhileImporting.add(e.getLocalizedMessage());
										e.printStackTrace();
									}
								} finally {
									try {
										newContents.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
									entry.releaseData(); // release data to prevent heap overflow :S
								}
							}
						}
					});
				} catch (Exception e) {
					Utilities.errorMessage(e, String.format(Messages.C4GroupImporter_ErrorImporting, resources[i].toString()));
					monitor.setCanceled(true);
					e.printStackTrace();
				}
				monitor.worked(1);
			}
		}
		finally {
			for (C4Group group : groups) {
				if (group != null)
					try {
						group.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}
		monitor.done();

	}

}
