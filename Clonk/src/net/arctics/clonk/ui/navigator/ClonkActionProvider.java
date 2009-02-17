package net.arctics.clonk.ui.navigator;

import org.eclipse.jface.action.IMenuManager;


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
		menu.add(new QuickExportAction("Quick Export"));
		menu.add(new ConvertOldCodeInBulkAction("Convert old code"));
	}

}
