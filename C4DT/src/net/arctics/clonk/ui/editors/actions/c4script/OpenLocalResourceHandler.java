package net.arctics.clonk.ui.editors.actions.c4script;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

public class OpenLocalResourceHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
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
		return null;
	}

}
