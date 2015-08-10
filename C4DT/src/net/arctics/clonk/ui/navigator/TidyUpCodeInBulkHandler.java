package net.arctics.clonk.ui.navigator;

import static java.util.Arrays.stream;
import static net.arctics.clonk.util.StreamUtil.ofType;
import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.printingException;
import static net.arctics.clonk.util.Utilities.runWithoutAutoBuild;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.ui.editors.actions.c4script.TidyUpCodeAction;
import net.arctics.clonk.util.UI;

public class TidyUpCodeInBulkHandler extends AbstractHandler {
	@Override
	public boolean isEnabled() {
		final IStructuredSelection sel = (IStructuredSelection) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		return stream(sel.toArray()).allMatch(o -> o instanceof IResource);
	}
	@SuppressWarnings("unchecked")
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ISelection selection = HandlerUtil.getCurrentSelection(event);
		final IStructuredSelection ssel = as(selection, IStructuredSelection.class);
		if (ssel == null)
			return null;
		if (!UI.confirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.TidyUpCodeInBulkHandler_ReallyConvert, null))
			return null;
		final List<IContainer> selectedContainers = ((List<Object>)ssel.toList()).stream().flatMap(obj -> {
			if (obj instanceof IProject)
				try {
					final IResource[] selectedResources = ((IProject)obj).members(IContainer.EXCLUDE_DERIVED);
					return ofType(stream(selectedResources), IContainer.class).filter(s -> !s.getName().startsWith(".")); //$NON-NLS-1$
				}
				catch (final CoreException ex) {
					ex.printStackTrace();
					return Stream.empty();
				}
			else if (obj instanceof IContainer)
				return stream(new IContainer[] { (IContainer)obj });
			else
				return Stream.empty();
		}).collect(Collectors.toList());
		if (!selectedContainers.isEmpty()) {
			final ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
			try {
				progressDialog.run(false, true, monitor -> {
					// first count how much to do
					class CountingVisitor implements IResourceVisitor {
						public int value = 0;
						@Override
						public boolean visit(IResource resource) {
							if (resource instanceof IFile && Script.get(resource, true) != null)
								value++;
							return true;
						};
					}
					final CountingVisitor counter = new CountingVisitor();
					selectedContainers.forEach(printingException(c -> c.accept(counter), CoreException.class));
					monitor.beginTask(Messages.TidyUpCodeInBulkAction_ConvertingCode, counter.value);
					runWithoutAutoBuild(() -> selectedContainers.forEach(printingException(c -> c.accept(resource -> {
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
								Core.instance().performActionsOnFileDocument(file, document -> {
									if (document != null)
										TidyUpCodeAction.converter().runOnDocument(script, document);
									return null;
								}, true);
								monitor.worked(1);
							}
						}
						return true;
					}), CoreException.class)));
					monitor.done();
				});
			} catch (final InvocationTargetException e) {
				e.printStackTrace();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}