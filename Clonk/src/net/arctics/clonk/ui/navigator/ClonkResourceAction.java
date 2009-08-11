package net.arctics.clonk.ui.navigator;

import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

public class ClonkResourceAction extends Action implements ISelectionChangedListener {

	private boolean enabled(ISelection selection) {
		IStructuredSelection sel = (IStructuredSelection) selection;
		return Utilities.allInstanceOf(sel.toArray(), IContainer.class);
	}
	
	public ClonkResourceAction() {
		super();
		Utilities.getProjectExplorer().getCommonViewer().addSelectionChangedListener(this);
	}
	
	public ClonkResourceAction(String text) {
		super (text);
	}

	public void selectionChanged(SelectionChangedEvent event) {
		setEnabled(enabled(event.getSelection()));
	}

}
