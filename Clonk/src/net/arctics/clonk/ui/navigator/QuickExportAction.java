package net.arctics.clonk.ui.navigator;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;

import net.arctics.clonk.preferences.PreferenceConstants;
import net.arctics.clonk.resource.c4group.C4GroupExporter;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

public class QuickExportAction extends ClonkResourceAction implements IHandler {

	public QuickExportAction() {
		super();
	}

	public QuickExportAction(String text) {
		super(text);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void runWithEvent(Event e) {
		super.run();
		if (PlatformUI.getWorkbench() == null ||
				PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ||
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService() == null ||
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection() == null)
			return;
		final ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (selection != null && selection instanceof TreeSelection) {
			IWorkbench wb = PlatformUI.getWorkbench();
			IProgressService ps = wb.getProgressService();
			try {
				TreeSelection tree = (TreeSelection) selection;					
				String c4groupPath = Utilities.getPreference(PreferenceConstants.C4GROUP_EXECUTABLE, "", null); //$NON-NLS-1$
				String gamePath = Utilities.getPreference(PreferenceConstants.GAME_PATH);
				Iterator it = tree.iterator();
				while (it.hasNext()) {
					Object obj = it.next();
					List<IContainer> selectedContainers = null;
					if (obj instanceof IProject) {
						try {
							IResource[] selectedResources = ((IProject)obj).members(IContainer.EXCLUDE_DERIVED);
							selectedContainers = new ArrayList<IContainer>();
							for(int i = 0; i < selectedResources.length;i++) {
								if (selectedResources[i] instanceof IContainer && !selectedResources[i].getName().startsWith(".")) //$NON-NLS-1$
									selectedContainers.add((IContainer) selectedResources[i]);
							}
						}
						catch (CoreException ex) {
							ex.printStackTrace();
						}
					}
					else if (obj instanceof IFolder) {
						selectedContainers = new ArrayList<IContainer>(1);
						selectedContainers.add((IContainer) obj);
					}
					if (selectedContainers != null) {
						final C4GroupExporter exporter = new C4GroupExporter(selectedContainers.toArray(new IContainer[selectedContainers.size()]), c4groupPath, gamePath);
						if (exporter.selectDestPaths())
							ps.busyCursorWhile(new IRunnableWithProgress() {
								public void run(IProgressMonitor pm) {
									exporter.export(pm);
								}
							});

					}
				}
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}

		//				return true;
	}

	public void addHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				runWithEvent(null);
			}
		});
		return null;
	}

	public void removeHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	public void run(IAction action) {
		runWithEvent(null);
	}

}