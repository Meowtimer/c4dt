package net.arctics.clonk.ui.navigator;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.resource.c4group.C4Group;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class ConvertLinkedGroupsHandler extends ClonkResourceHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			final ISelection selection = HandlerUtil.getCurrentSelection(event);
			Map<IContainer, List<File>> filesToReimport = new HashMap<IContainer, List<File>>();
			if (MessageDialog.openQuestion(
					HandlerUtil.getActiveShell(event),
					Messages.ConvertLinkedGroupsHandler_ConfirmationTitle,
					Messages.ConvertLinkedGroupsHandler_ConfirmationText
			)) {
				for (Object sel : ((IStructuredSelection)selection).toList()) {
					if (sel instanceof IContainer) {
						IContainer container = (IContainer) sel;
						IFileStore store = EFS.getStore(container.getLocationURI());
						if (store instanceof C4Group) {
							C4Group group = (C4Group) store;
							IContainer parent = container.getParent();
							List<File> list = filesToReimport.get(parent);
							if (list == null) {
								list = new LinkedList<File>();
								filesToReimport.put(parent, list);
							}
							list.add(group.getOrigin());
							container.delete(true, new NullProgressMonitor());
						}
					}
				}
				for (Map.Entry<IContainer, List<File>> entry : filesToReimport.entrySet()) {
					QuickImportHandler.importFiles(
							HandlerUtil.getActiveShell(event),
							entry.getKey(), entry.getValue().toArray(new File[entry.getValue().size()])
					);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}

}
