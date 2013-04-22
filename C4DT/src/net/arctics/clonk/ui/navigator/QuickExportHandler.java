package net.arctics.clonk.ui.navigator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.arctics.clonk.c4group.C4GroupExporter;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

public class QuickExportHandler extends ClonkResourceHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			@SuppressWarnings("rawtypes")
			public void run() {
				try {
					List<IContainer> selectedContainers = new ArrayList<IContainer>();

					IWorkbenchPart part = HandlerUtil.getActivePart(event);
					if (part instanceof EditorPart && ((EditorPart)part).getEditorInput() instanceof FileEditorInput) {
						IFile file = ((FileEditorInput)((EditorPart)part).getEditorInput()).getFile();
						IContainer r; 
						for (r = file.getParent(); r != null && !(r.getParent() instanceof IProject); r = r.getParent());
						if (r != null)
							selectedContainers.add(r);
					} else {
						final ISelection selection = HandlerUtil.getCurrentSelection(event);
						if (selection != null && selection instanceof TreeSelection) {
							TreeSelection tree = (TreeSelection) selection;
							Iterator it = tree.iterator();
							while (it.hasNext()) {
								Object obj = it.next();
								if (obj instanceof IProject)
									try {
										IResource[] selectedResources = ((IProject)obj).members(IContainer.EXCLUDE_DERIVED);
										for(int i = 0; i < selectedResources.length;i++)
											if (selectedResources[i] instanceof IContainer && !selectedResources[i].getName().startsWith(".")) //$NON-NLS-1$
												selectedContainers.add((IContainer) selectedResources[i]);
									}
									catch (CoreException ex) {
										ex.printStackTrace();
									}
								else if (obj instanceof IFolder)
									selectedContainers.add((IContainer) obj);
								else if (obj instanceof IFile) {
									IFile file = (IFile)obj;
									IContainer r;
									for (r = file.getParent(); r != null && !(r.getParent() instanceof IProject); r = r.getParent());
									if (r != null)
										selectedContainers.add(r);
								}
							}
						}
					}
					if (selectedContainers.size() > 0) {
						final C4GroupExporter exporter = new C4GroupExporter(selectedContainers.toArray(new IContainer[selectedContainers.size()]), null);
						if (exporter.selectDestPaths()) {
							final ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(HandlerUtil.getActiveWorkbenchWindow(event).getShell());
							progressDialog.run(false, true, exporter);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		return null;
	}

}