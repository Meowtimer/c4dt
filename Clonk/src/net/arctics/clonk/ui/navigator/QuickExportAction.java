/**
 * 
 */
package net.arctics.clonk.ui.navigator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.preferences.PreferenceConstants;
import net.arctics.clonk.resource.c4group.C4GroupExporter;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;

public class QuickExportAction extends ClonkResourceAction {
		QuickExportAction(String text) {
			super(text);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void runWithEvent(Event e) {
			super.run();
			if (PlatformUI.getWorkbench() == null ||
					PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ||
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService() == null ||
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection() == null)
				return;
			ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
			if (selection != null && selection instanceof TreeSelection) {
				TreeSelection tree = (TreeSelection) selection;
				IPreferencesService service = Platform.getPreferencesService();
				String c4groupPath = service.getString(ClonkCore.PLUGIN_ID, PreferenceConstants.C4GROUP_EXECUTABLE, "", null);
				String gamePath = service.getString(ClonkCore.PLUGIN_ID, PreferenceConstants.GAME_PATH, null, null);
				Iterator it = tree.iterator();
				while (it.hasNext()) {
					Object obj = it.next();
					List<IContainer> selectedContainers = null;
					if (obj instanceof IProject) {
						try {
							IResource[] selectedResources = ((IProject)obj).members(IContainer.EXCLUDE_DERIVED);
							selectedContainers = new ArrayList<IContainer>();
							for(int i = 0; i < selectedResources.length;i++) {
								if (selectedResources[i] instanceof IContainer && !selectedResources[i].getName().startsWith("."))
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
						C4GroupExporter exporter = new C4GroupExporter(selectedContainers.toArray(new IContainer[selectedContainers.size()]),c4groupPath,gamePath);
						exporter.export(null);
					}
				}
			}

//				return true;
		}
	}