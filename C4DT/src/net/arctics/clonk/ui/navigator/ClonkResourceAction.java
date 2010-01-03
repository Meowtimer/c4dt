package net.arctics.clonk.ui.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public abstract class ClonkResourceAction extends AbstractHandler implements IWorkbenchWindowActionDelegate {
	
	public ClonkResourceAction() {
		super();
		//Utilities.getProjectExplorer().getCommonViewer().addSelectionChangedListener(this);
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
		// TODO Auto-generated method stub
		
	}

}
