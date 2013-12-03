package net.arctics.clonk.c4group;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import net.arctics.clonk.Core;
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

	public static final String IMPORTING_FOLDER = "C4GroupImport";
	
	private final File[] groupFiles;
	private final IContainer destination;

	public C4GroupImporter(final File[] groupsToImport, final IContainer destination) {
		super();
		groupFiles = groupsToImport;
		this.destination = destination;
		determineWhatGroupsNeedToBeMovedAndDeleted();
	}
	
	/**
	 * When importing a group inside the project folder the group file needs to be moved to some temporary location first and after importing deleted
	 */
	private void determineWhatGroupsNeedToBeMovedAndDeleted() {
		final File destinationFile = new File(destination.getLocation().toOSString());
		File importingFolder = null;
		for (int i = 0; i < groupFiles.length; i++) {
			final File gf = groupFiles[i];
			if (new File(destinationFile, gf.getName()).equals(gf)) {
				if (importingFolder == null)
					importingFolder = Core.instance().requestFolderInStateLocation(IMPORTING_FOLDER);
				final File fileMoveDestination = new File(importingFolder, gf.getName());
				if (gf.renameTo(fileMoveDestination))
					groupFiles[i] = fileMoveDestination;
				else
					System.out.println("Failed to move " + gf.toString() + "!");
			}
		}
	}

	@Override
	protected void execute(final IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		final C4Group[] groups = new C4Group[groupFiles.length];
		monitor.beginTask(Messages.C4GroupImporter_ImportingFiles, groups.length);
		try {
			for(int i = 0; i < groupFiles.length;i++) {
				if (monitor.isCanceled())
					return;
				try {
					groups[i] = C4Group.openFile(groupFiles[i]);
					monitor.subTask(String.format(Messages.C4GroupImporter_Importing, groups[i].getName()));
					final List<String> errorsWhileImporting = new LinkedList<String>();
					groups[i].readIntoMemory(true, new C4GroupHeaderFilterBase() {
						private IContainer currentContainer = destination;
						private C4Group currentGroup;

						@Override
						public boolean accepts(final C4GroupEntryHeader header, final C4Group context) {
							return true; // import whole group
						}

						@Override
						public void processGroupItem(final C4GroupItem item) throws CoreException {
							for (; currentGroup != item.parentGroup(); currentGroup = currentGroup.parentGroup(), currentContainer = currentContainer.getParent());
							if (item instanceof C4Group) {
								final C4Group group = (C4Group)item;
								/*if (group.getParentGroup() == null)
									monitor.subTask(String.format(Messages.C4GroupImporter_ImportTask, group.getName()));*/
								final IFolder newFolder = currentContainer.getFolder(new Path(group.getName()));
								if (!newFolder.exists())
									newFolder.create(IResource.NONE, true, monitor);
								currentContainer = newFolder;
								currentGroup = group;
							}
							else {
								final C4GroupFile entry = (C4GroupFile)item;
								final IFile newFile = currentContainer.getFile(new Path(entry.getName()));
								final InputStream newContents = entry.getContents();
								try {
									try {
										if (newFile.exists())
											newFile.setContents(newContents, 0, monitor);
										else
											newFile.create(entry.getContents(), IResource.NONE, monitor);
									} catch (final CoreException e) {
										errorsWhileImporting.add(e.getLocalizedMessage());
										e.printStackTrace();
									}
								} finally {
									try {
										newContents.close();
									} catch (final IOException e) {
										e.printStackTrace();
									}
									entry.releaseData(); // release data to prevent heap overflow :S
								}
							}
						}
					});
				} catch (final Exception e) {
					Utilities.errorMessage(e, String.format(Messages.C4GroupImporter_ErrorImporting, groupFiles[i].toString()));
					monitor.setCanceled(true);
					e.printStackTrace();
				}
				monitor.worked(1);
			}
		}
		finally {
			for (final C4Group group : groups)
				if (group != null)
					try {
						group.close();
					} catch (final IOException e) {
						e.printStackTrace();
					}
		}
		monitor.done();

	}

}
