package net.arctics.clonk.ui.navigator;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import net.arctics.clonk.filesystem.C4GroupFileSystem;
import net.arctics.clonk.preferences.ClonkPreferences;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class LinkC4GroupFileHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection sel = HandlerUtil.getCurrentSelection(event);
		if (sel instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection)sel).getFirstElement();
			if (obj instanceof IProject) {
				IProject proj = (IProject) obj;
				FileDialog fileDialog = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN+SWT.MULTI);
				fileDialog.setFilterPath(ClonkPreferences.getPreferenceOrDefault(ClonkPreferences.GAME_PATH));
				String filePath;
				if ((filePath = fileDialog.open()) != null) {
					File f = new File(filePath);
					linkC4GroupFile(proj, f);
				}
			}
		}
		return null;
	}

	public static IFolder linkC4GroupFile(IProject proj, final File f) {
		final IFolder linkedFolder = proj.getFolder(f.getName());
		final ProgressMonitorDialog dialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		try {
			dialog.run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						linkedFolder.createLink(new URI("c4group", C4GroupFileSystem.replaceSpecialChars(f.getAbsolutePath()), null), 0, dialog.getProgressMonitor());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return linkedFolder;
	}

}
