package net.arctics.clonk.ui.navigator;

import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;

public class ClonkResourceAction extends Action {
	
	ClonkResourceAction(String text) {
		super (text);
	}
	
	@Override
	public boolean isEnabled() {
		IStructuredSelection sel = (IStructuredSelection) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		return Utilities.allInstanceOf(sel.toArray(), IResource.class);
	}
}
