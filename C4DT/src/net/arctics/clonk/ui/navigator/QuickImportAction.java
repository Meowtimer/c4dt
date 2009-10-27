package net.arctics.clonk.ui.navigator;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import net.arctics.clonk.preferences.PreferenceConstants;
import net.arctics.clonk.ui.wizards.C4GroupImporter;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PlatformUI;

public class QuickImportAction extends ClonkResourceAction implements IHandler {
	public QuickImportAction() {
		super();
	}

	public QuickImportAction(String text) {
		super(text);
	}

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (PlatformUI.getWorkbench() == null ||
						PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ||
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService() == null ||
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection() == null)
					return;
				final ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
				if (selection.isEmpty() || !(selection instanceof IStructuredSelection))
					return;
				final IStructuredSelection ssel = (IStructuredSelection) selection;
				if (!(ssel.getFirstElement() instanceof IContainer))
					return;
				IContainer container = (IContainer) ssel.getFirstElement();
				
				String gamePath = PreferenceConstants.getPreference(PreferenceConstants.GAME_PATH);
				
				final FileDialog fileDialog = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN+SWT.MULTI);
				fileDialog.setFilterPath(gamePath);
				fileDialog.setText(String.format(Messages.QuickImportAction_SelectFiles, container.getName()));
				fileDialog.setFilterExtensions(new String[] {UI.FILEDIALOG_CLONK_FILTER, "*.*"}); //$NON-NLS-1$
				if (fileDialog.open() != null) {
					File[] files = Utilities.map(fileDialog.getFileNames(), File.class, new IConverter<String, File>() { 
						@Override
						public File convert(String fileName) {
							return new File(fileDialog.getFilterPath()+"/"+fileName); //$NON-NLS-1$
						}
					});
					C4GroupImporter importer = new C4GroupImporter(files, container);
					final ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
					try {
						progressDialog.run(false, true, importer);
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
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
