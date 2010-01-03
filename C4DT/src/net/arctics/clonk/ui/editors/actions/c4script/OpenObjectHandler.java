package net.arctics.clonk.ui.editors.actions.c4script;

import net.arctics.clonk.ClonkCore;

import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.ui.OpenObjectDialog;
import net.arctics.clonk.ui.editors.ClonkTextEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;

public class OpenObjectHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		OpenObjectDialog dialog = new OpenObjectDialog(ClonkCore.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell());
		switch (dialog.open()) {
		case Window.OK:
			for (C4Object o : dialog.getSelectedObjects()) {
				try {
					ClonkTextEditor.openDeclaration(o);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			break;
		}
		return null;
	}

}
