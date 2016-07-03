package net.arctics.clonk.ui.navigator;

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
	public ClonkActionProvider() {}
	@Override
	public void init(final ICommonActionExtensionSite site) {
		super.init(site);
		final ICommonViewerSite viewSite = site.getViewSite();
		if(viewSite instanceof ICommonViewerWorkbenchSite) {
			final ICommonViewerWorkbenchSite workbenchSite = 
				(ICommonViewerWorkbenchSite) viewSite;
			openAction = 
				new OpenSpecialItemAction(workbenchSite.getPage(), 
					workbenchSite.getSelectionProvider());
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	public void fillContextMenu(final IMenuManager menu) {
		super.fillContextMenu(menu);
		if (openAction.isEnabled())
			menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openAction);
	}
	@Override
	public void fillActionBars(final IActionBars actionBars) {
		super.fillActionBars(actionBars);
		if (openAction.isEnabled())
			actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openAction);
	}
}