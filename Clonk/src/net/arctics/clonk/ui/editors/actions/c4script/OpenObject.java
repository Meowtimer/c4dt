package net.arctics.clonk.ui.editors.actions.c4script;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.ui.OpenObjectDialog;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class OpenObject extends Action implements
		IWorkbenchWindowActionDelegate, IHandler {

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

	public void addHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub
		
	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		run();
		return null;
	}

	public void removeHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub
		
	}

}
