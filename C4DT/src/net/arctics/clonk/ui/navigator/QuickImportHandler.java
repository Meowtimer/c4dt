package net.arctics.clonk.ui.navigator;

import java.io.File;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.C4GroupImporter;
import net.arctics.clonk.util.ArrayUtil;

public class QuickImportHandler extends ClonkResourceHandler {
	@Override
	public void addHandlerListener(final IHandlerListener handlerListener) {}
	public static File[] selectFiles(final String title, final IContainer container, final boolean noMulti) {
		final FileDialog dlg = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SHEET+SWT.OPEN+(noMulti?0:SWT.MULTI));
		dlg.setFilterPath(ClonkProjectNature.engineFromResource(container).settings().gamePath);
		dlg.setText(String.format(title, container.getName()));
		dlg.setFilterExtensions(new String[] {ClonkProjectNature.engineFromResource(container).settings().fileDialogFilterForGroupFiles(), "*.*"}); //$NON-NLS-1$
		return dlg.open() != null ? ArrayUtil.map(dlg.getFileNames(), File.class, new FullPathConverter(dlg)) : new File[0];
	}
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		Display.getDefault().asyncExec(() -> {
			final ISelection selection = HandlerUtil.getCurrentSelection(event);
			if (selection == null || selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
				return;
			}
			final IStructuredSelection ssel = (IStructuredSelection) selection;
			if (!(ssel.getFirstElement() instanceof IContainer)) {
				return;
			}
			final IContainer container = (IContainer) ssel.getFirstElement();
			final File[] files = selectFiles(Messages.QuickImportAction_SelectFiles, container, false);
			if (files != null) {
				importFiles(HandlerUtil.getActiveShell(event), container, files);
			}
		});
		return null;
	}
	public static void importFiles(final Shell shell, final IContainer container, final File... files) {
		final C4GroupImporter importer = new C4GroupImporter(files, container);
		final ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(shell);
		try {
			progressDialog.run(false, true, importer);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void removeHandlerListener(final IHandlerListener handlerListener) {}
}