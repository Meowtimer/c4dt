package net.arctics.clonk.ui.wizards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.InvalidDataException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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
	protected void execute(IProgressMonitor monitor) throws CoreException,
	InvocationTargetException, InterruptedException {
		int totalWork = 0;
		C4Group[] groups = new C4Group[resources.length];
		try {
			try {
				for(int i = 0; i < resources.length;i++) {
					groups[i] = C4Group.OpenFile(resources[i]);
					groups[i].open(true);
					totalWork += groups[i].computeSize();
				}
				monitor.beginTask("Import", totalWork);
				for(C4Group group : groups) {
					if (monitor.isCanceled()) break;
					monitor.subTask(group.getName());
					group.open(true);
					group.extractToFilesystem(destination, monitor);
					group.close();
					group = null; // destruct
					//	        	monitor.worked();
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
