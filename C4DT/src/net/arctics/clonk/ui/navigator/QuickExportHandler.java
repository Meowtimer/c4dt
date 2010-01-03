package net.arctics.clonk.ui.navigator;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;

import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.c4group.C4GroupExporter;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

public class QuickExportHandler extends ClonkResourceHandler implements IHandler {

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		Display.getDefault().asyncExec(new Runnable() {
			@SuppressWarnings("rawtypes")
			public void run() {
				final ISelection selection = HandlerUtil.getCurrentSelection(event);
				if (selection != null && selection instanceof TreeSelection) {
					try {
						TreeSelection tree = (TreeSelection) selection;					
						String c4groupPath = ClonkPreferences.getPreference(ClonkPreferences.C4GROUP_EXECUTABLE, "", null); //$NON-NLS-1$
						String gamePath = ClonkPreferences.getPreference(ClonkPreferences.GAME_PATH);
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
								if (exporter.selectDestPaths()) {
									final ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(HandlerUtil.getActiveWorkbenchWindow(event).getShell());
									progressDialog.run(false, true, exporter);
								}
							}
						}
					} catch (Exception exc) {
						exc.printStackTrace();
					}
				}
			}
		});
		return null;
	}

}