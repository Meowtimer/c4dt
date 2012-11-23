package net.arctics.clonk.ui.navigator.actions;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.ui.OpenDefinitionDialog;
import net.arctics.clonk.ui.editors.ClonkTextEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;

public class OpenDefinitionHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		OpenDefinitionDialog dialog = new OpenDefinitionDialog();
		switch (dialog.open()) {
		case Window.OK:
			for (Definition o : dialog.selectedDefinitions())
				try {
					ClonkTextEditor.openDeclaration(o);
				} catch (Exception e) {
					e.printStackTrace();
				}
			break;
		}
		return null;
	}

}
