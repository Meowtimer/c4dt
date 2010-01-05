package net.arctics.clonk.ui.navigator;

import java.io.File;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.ui.wizards.C4GroupImporter;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class QuickImportHandler extends ClonkResourceHandler {

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {}
	
	public static File[] selectFiles(String title, IContainer container, boolean noMulti) {
		final FileDialog fileDialog = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN+(noMulti?0:SWT.MULTI));
		fileDialog.setFilterPath(ClonkPreferences.getPreference(ClonkPreferences.GAME_PATH));
		fileDialog.setText(String.format(title, container.getName()));
		fileDialog.setFilterExtensions(new String[] {UI.FILEDIALOG_CLONK_FILTER, "*.*"}); //$NON-NLS-1$
		if (fileDialog.open() != null) {
			return Utilities.map(fileDialog.getFileNames(), File.class, new IConverter<String, File>() { 
				@Override
				public File convert(String fileName) {
					return new File(fileDialog.getFilterPath()+"/"+fileName); //$NON-NLS-1$
				}
			});
		} else {
			return null;
		}
	}

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				final ISelection selection = HandlerUtil.getCurrentSelection(event);
				if (selection == null || selection.isEmpty() || !(selection instanceof IStructuredSelection))
					return;
				final IStructuredSelection ssel = (IStructuredSelection) selection;
				if (!(ssel.getFirstElement() instanceof IContainer))
					return;
				IContainer container = (IContainer) ssel.getFirstElement();
				
				File[] files = selectFiles(Messages.QuickImportAction_SelectFiles, container, false);
				if (files != null) {
					C4GroupImporter importer = new C4GroupImporter(files, container);
					final ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(HandlerUtil.getActiveWorkbenchWindow(event).getShell());
					try {
						progressDialog.run(false, true, importer);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		return null;
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {}
	
}
