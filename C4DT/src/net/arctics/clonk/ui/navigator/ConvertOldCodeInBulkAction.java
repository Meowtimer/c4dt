package net.arctics.clonk.ui.navigator;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.ui.editors.actions.c4script.ConvertOldCodeToNewCodeAction;
import net.arctics.clonk.ui.editors.actions.c4script.ConvertOldCodeToNewCodeAction.FunctionStatements;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

public class ConvertOldCodeInBulkAction extends Action {
	ConvertOldCodeInBulkAction(String text) {
		super(text);
	}

	@Override
	public boolean isEnabled() {
		IStructuredSelection sel = (IStructuredSelection) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		return Utilities.allInstanceOf(sel.toArray(), IResource.class);
	}

	private int counter;

	@Override
	public void runWithEvent(Event event) {
		super.run();
		if (PlatformUI.getWorkbench() == null ||
				PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ||
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService() == null ||
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection() == null)
			return;
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (selection != null && selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			Iterator<?> it = sel.iterator();
			final List<IContainer> selectedContainers = new LinkedList<IContainer>();
			while (it.hasNext()) {
				Object obj = it.next();
				if (obj instanceof IProject) {
					try {
						IResource[] selectedResources = ((IProject)obj).members(IContainer.EXCLUDE_DERIVED);
						for(int i = 0; i < selectedResources.length;i++) {
							if (selectedResources[i] instanceof IContainer && !selectedResources[i].getName().startsWith(".")) //$NON-NLS-1$
								selectedContainers.add((IContainer) selectedResources[i]);
						}
					}
					catch (CoreException ex) {
						ex.printStackTrace();
					}
				}
				else if (obj instanceof IFolder) {
					selectedContainers.add((IContainer) obj);
				}
			}
			if (selectedContainers.size() > 0) {
				ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
				try {
					progressDialog.run(true, true, new IRunnableWithProgress() {
						@Override
						public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							// first count how much to do
							{
								counter = 0;
								IResourceVisitor countingVisitor = new IResourceVisitor() {
									@Override
									public boolean visit(IResource resource) throws CoreException {
										if (resource instanceof IFile && Utilities.getScriptForFile((IFile) resource) != null)
											counter++;
										return true;
									}
								};
								for (IContainer container : selectedContainers) {
									try {
										container.accept(countingVisitor);
									} catch (CoreException e) {
										e.printStackTrace();
									}
								}
							}
							monitor.beginTask(Messages.ConvertOldCodeInBulkAction_ConvertingCode, counter);
							for (IContainer container : selectedContainers) {
								try {
									final TextFileDocumentProvider textFileDocProvider = ClonkCore.getDefault().getTextFileDocumentProvider();
									final List<IFile> failedSaves = new LinkedList<IFile>();
									container.accept(new IResourceVisitor() {
										public boolean visit(IResource resource) throws CoreException {
											if (resource instanceof IFile) {
												IFile file = (IFile) resource;
												C4ScriptBase script = Utilities.getScriptForFile(file);
												if (script != null) {
													C4ScriptParser parser = new C4ScriptParser(file, script);
													LinkedList<FunctionStatements> statements = new LinkedList<FunctionStatements>();
													parser.setExpressionListener(ConvertOldCodeToNewCodeAction.expressionCollector(null, statements, 0));
													try {
														parser.parse();
													} catch (ParsingException e1) {
														e1.printStackTrace();
													}
													textFileDocProvider.connect(file);
													try {
														IDocument document = textFileDocProvider.getDocument(file);

														if (document != null)
															ConvertOldCodeToNewCodeAction.runOnDocument(parser, new TextSelection(document, 0, 0), document, statements);

														try {
															textFileDocProvider.setEncoding(document, textFileDocProvider.getDefaultEncoding());
															textFileDocProvider.saveDocument(null, file, document, true);
														} catch (CoreException e) {
															e.printStackTrace();
															failedSaves.add(file);
														}
													} finally {
														textFileDocProvider.disconnect(file);
													}
													monitor.worked(1);
												}
											}
											else if (resource instanceof IContainer) {
												if (monitor.isCanceled())
													return false;
											}
											return true;
										}
									});
									// TODO: do something with failedSaves
								} catch (CoreException e) {
									e.printStackTrace();
								}
							}
							monitor.done();
						}
					});
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}