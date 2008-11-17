package net.arctics.clonk.ui.editors.actions;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.ui.OpenObjectDialog;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class OpenObject extends Action implements
		IWorkbenchWindowActionDelegate {

	public void dispose() {
		// nothing to dispose
	}

	public void init(IWorkbenchWindow window) {
		// nothing todo
	}

	public void run() {
		OpenObjectDialog dialog = new OpenObjectDialog(ClonkCore.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell());
		dialog.open();
	}

	public void run(IAction action) {
		run();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// who cares?
	}

}
