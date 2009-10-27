package net.arctics.clonk.ui.editors.actions.c4script;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

public class OpenLocalResourceAction extends Action implements IWorkbenchWindowActionDelegate, IHandler {

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void run() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (page != null && page.getActiveEditor() != null && page.getActiveEditor().getEditorInput() instanceof FileEditorInput) {
			IContainer container = ((FileEditorInput)page.getActiveEditor().getEditorInput()).getFile().getParent();
			FilteredResourcesSelectionDialog dialog = new FilteredResourcesSelectionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), true, container, IResource.FILE);
			switch (dialog.open()) {
			case Window.OK:
				for (Object f : dialog.getResult()) {
					if (f instanceof IFile) {
						try {
							IDE.openEditor(page, (IFile) f);
						} catch (PartInitException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	@Override
	public void run(IAction action) {
		run();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		run();
		return null;
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub
		
	}

}
