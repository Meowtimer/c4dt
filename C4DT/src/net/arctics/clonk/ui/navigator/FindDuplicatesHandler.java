package net.arctics.clonk.ui.navigator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.ui.search.FindDuplicatesQuery;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class FindDuplicatesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			Iterator<?> it = ssel.iterator();
			final Set<ScriptBase> scripts = new HashSet<ScriptBase>();
			IResourceVisitor visitor = new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					ScriptBase script = ScriptBase.get(resource, false);
					if (script != null)
						scripts.add(script);
					return true;
				}
			}; 
			while (it.hasNext()) {
				Object obj = it.next();
				if (obj instanceof IResource) {
					IResource res = (IResource) obj;
					try {
						res.accept(visitor);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
			NewSearchUI.runQueryInBackground(FindDuplicatesQuery.queryWithScripts(scripts));
		}
		return null;
	}

}
