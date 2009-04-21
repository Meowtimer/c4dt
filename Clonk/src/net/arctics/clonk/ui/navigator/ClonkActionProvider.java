package net.arctics.clonk.ui.navigator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;


public class ClonkActionProvider extends CommonActionProvider {

	private OpenSpecialItemAction openAction;
	private QuickExportAction quickExportAction;
	private ConvertOldCodeInBulkAction convertCodeAction;
	
	public ClonkActionProvider() {

	}
	
	@Override
	public void init(ICommonActionExtensionSite site) {
		super.init(site);
		ICommonViewerSite viewSite = site.getViewSite();
		if(viewSite instanceof ICommonViewerWorkbenchSite) {
			ICommonViewerWorkbenchSite workbenchSite = 
				(ICommonViewerWorkbenchSite) viewSite;
			openAction = 
				new OpenSpecialItemAction(workbenchSite.getPage(), 
					workbenchSite.getSelectionProvider());
		}
		quickExportAction = new QuickExportAction("Quick Export");
		convertCodeAction = new ConvertOldCodeInBulkAction("Convert old code");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		for (IAction a : new IAction[]{quickExportAction, convertCodeAction})
			if (a.isEnabled())
				menu.add(a);
		if (openAction.isEnabled())
			menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openAction);
	}
	
	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		if (openAction.isEnabled())
			actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openAction);
	}

}
