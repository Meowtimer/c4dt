package net.arctics.clonk.ui.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public abstract class ClonkResourceHandler extends AbstractHandler implements IWorkbenchWindowActionDelegate {
	
	public ClonkResourceHandler() {
		super();
		//UI.getProjectExplorer().getCommonViewer().addSelectionChangedListener(this);
	}
	
	@Override
	public void dispose() {
	}

	@Override
	public void init(final IWorkbenchWindow window) {
	}

	@Override
	public void run(final IAction action) {
	}

	@Override
	public void selectionChanged(final IAction action, final ISelection selection) {
		// TODO Auto-generated method stub
		
	}

}
