package net.arctics.clonk.ui.navigator;

import java.io.File;
import java.net.URI;

import net.arctics.clonk.c4group.C4GroupFileSystem;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class LinkC4GroupFileHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ISelection sel = HandlerUtil.getCurrentSelection(event);
		if (sel instanceof IStructuredSelection) {
			final Object obj = ((IStructuredSelection)sel).getFirstElement();
			if (obj instanceof IProject) {
				final IProject proj = (IProject) obj;
				for (final File f : QuickImportHandler.selectFiles("Select C4Group files", proj, false))
					linkC4GroupFile(proj, f);
			}
		}
		return null;
	}

	public static IFolder linkC4GroupFile(final IProject proj, final File f) {
		final IFolder linkedFolder = proj.getFolder(f.getName());
		final ProgressMonitorDialog dialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		try {
			dialog.run(false, false, monitor -> {
				try {
					linkedFolder.createLink(new URI(C4GroupFileSystem.SCHEME, C4GroupFileSystem.replaceSpecialChars(f.getAbsolutePath()), null), 0, dialog.getProgressMonitor());
				} catch (final Exception e) {
					e.printStackTrace();
				}
			});
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return linkedFolder;
	}

}
