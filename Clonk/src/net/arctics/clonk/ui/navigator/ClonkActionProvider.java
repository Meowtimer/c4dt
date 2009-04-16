package net.arctics.clonk.ui.navigator;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonActionProvider;


public class ClonkActionProvider extends CommonActionProvider {

	public ClonkActionProvider() {

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		menu.add(new QuickExportAction("Quick Export"));
		menu.add(new ConvertOldCodeInBulkAction("Convert old code"));
	}
	
	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
	}

}
