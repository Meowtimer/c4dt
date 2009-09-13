package net.arctics.clonk.ui.navigator;

import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class ClonkResourceAction extends Action implements IWorkbenchWindowActionDelegate {

	private boolean enabled(ISelection selection) {
		IStructuredSelection sel = (IStructuredSelection) selection;
		return Utilities.allInstanceOf(sel.toArray(), IContainer.class);
	}
	
	public ClonkResourceAction() {
		super();
		//Utilities.getProjectExplorer().getCommonViewer().addSelectionChangedListener(this);
	}
	
	public ClonkResourceAction(String text) {
		super (text);
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
	}

	@Override
	public void run(IAction action) {
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		setEnabled(enabled(selection));
	}

}
