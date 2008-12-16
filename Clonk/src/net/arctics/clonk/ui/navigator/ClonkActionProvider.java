package net.arctics.clonk.ui.navigator;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.preferences.PreferenceConstants;
import net.arctics.clonk.resource.c4group.C4GroupExporter;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;


public class ClonkActionProvider extends org.eclipse.ui.navigator.CommonActionProvider {

	public ClonkActionProvider() {

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
//		menu.appendToGroup("group.additions",new Action("Quick Export") {
		menu.add(new Action("Quick Export") {
			@Override
			public void runWithEvent(Event e) {
				super.run();
				ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
				if (selection != null && selection instanceof TreeSelection) {
					IProject project = null;
					TreeSelection tree = (TreeSelection) selection;
					if (tree.getFirstElement() instanceof IResource) {
						project = ((IResource)tree.getFirstElement()).getProject();
						IPreferencesService service = Platform.getPreferencesService();
						String c4groupPath = service.getString(ClonkCore.PLUGIN_ID, PreferenceConstants.C4GROUP_EXECUTABLE, "", null);
						String gamePath = service.getString(ClonkCore.PLUGIN_ID, PreferenceConstants.GAME_PATH, null, null);
						try {
							IResource[] selectedResources = project.members(IContainer.EXCLUDE_DERIVED);
							List<IContainer> selectedContainers = new ArrayList<IContainer>();
							for(int i = 0; i < selectedResources.length;i++) {
								if (selectedResources[i] instanceof IContainer && !selectedResources[i].getName().startsWith("."))
									selectedContainers.add((IContainer) selectedResources[i]);
							}
							C4GroupExporter exporter = new C4GroupExporter(selectedContainers.toArray(new IContainer[selectedContainers.size()]),c4groupPath,gamePath);
							exporter.export(null);
						}
						catch (CoreException ex) {
							ex.printStackTrace();
						}
					}
				}

//				return true;
			}
		});
	}
	
	

}
