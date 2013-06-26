package net.arctics.clonk.ui.navigator;

import static net.arctics.clonk.util.Utilities.runWithoutAutoBuild;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.Core.IDocumentAction;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.ui.editors.actions.c4script.TidyUpCodeAction;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class TidyUpCodeInBulkHandler extends AbstractHandler {

	@Override
	public boolean isEnabled() {
		final IStructuredSelection sel = (IStructuredSelection) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		return Utilities.allInstanceOf(sel.toArray(), IResource.class);
	}

	private int counter;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			if (!UI.confirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.TidyUpCodeInBulkHandler_ReallyConvert, null))
				return null;
			final IStructuredSelection sel = (IStructuredSelection) selection;
			final Iterator<?> it = sel.iterator();
			final List<IContainer> selectedContainers = new LinkedList<IContainer>();
			while (it.hasNext()) {
				final Object obj = it.next();
				if (obj instanceof IProject)
					try {
						final IResource[] selectedResources = ((IProject)obj).members(IContainer.EXCLUDE_DERIVED);
						for(int i = 0; i < selectedResources.length;i++)
							if (selectedResources[i] instanceof IContainer && !selectedResources[i].getName().startsWith(".")) //$NON-NLS-1$
								selectedContainers.add((IContainer) selectedResources[i]);
					}
					catch (final CoreException ex) {
						ex.printStackTrace();
					}
				else if (obj instanceof IFolder)
					selectedContainers.add((IContainer) obj);
			}
			if (selectedContainers.size() > 0) {
				final ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
				try {
					progressDialog.run(false, true, new IRunnableWithProgress() {
						@Override
						public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							// first count how much to do
							{
								counter = 0;
								final IResourceVisitor countingVisitor = new IResourceVisitor() {
									@Override
									public boolean visit(IResource resource) throws CoreException {
										if (resource instanceof IFile && Script.get(resource, true) != null)
											counter++;
										return true;
									}
								};
								for (final IContainer container : selectedContainers)
									try {
										container.accept(countingVisitor);
									} catch (final CoreException e) {
										e.printStackTrace();
									}
							}
							runWithoutAutoBuild(new Runnable() { @Override public void run() {
								monitor.beginTask(Messages.TidyUpCodeInBulkAction_ConvertingCode, counter);
								for (final IContainer container : selectedContainers)
									try {
										container.accept(new IResourceVisitor() {
											@Override
											public boolean visit(IResource resource) throws CoreException {
												if (monitor.isCanceled())
													return false;
												if (resource instanceof IFile) {
													final IFile file = (IFile) resource;
													final Script script = Script.get(file, true);
													if (script != null) {
														final ScriptParser parser = new ScriptParser(file, script, null);
														try {
															parser.parse();
														} catch (final ProblemException e1) {
															e1.printStackTrace();
														}
														Core.instance().performActionsOnFileDocument(file, new IDocumentAction<Void>() {
															@Override
															public Void run(IDocument document) {
																if (document != null)
																	TidyUpCodeAction.converter().runOnDocument(script, document);
																return null;
															}
														}, true);
														monitor.worked(1);
													}
												}
												else if (resource instanceof IContainer)
													if (monitor.isCanceled())
														return false;
												return true;
											}
										});
										// TODO: do something with failedSaves
									} catch (final CoreException e) {
										e.printStackTrace();
									}
								monitor.done();
							}});
						}
					});
				} catch (final InvocationTargetException e) {
					e.printStackTrace();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
}