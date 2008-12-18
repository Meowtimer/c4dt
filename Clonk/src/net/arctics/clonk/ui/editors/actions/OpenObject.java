package net.arctics.clonk.ui.editors.actions;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.ui.OpenObjectDialog;
import net.arctics.clonk.ui.editors.C4ScriptEditor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
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
		switch (dialog.open()) {
		case Window.OK:
			for (C4Object o : dialog.getSelectedObjects()) {
				try {
					C4ScriptEditor.openDeclaration(o);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			break;
		}
	}

	public void run(IAction action) {
		run();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// who cares?
	}

}
