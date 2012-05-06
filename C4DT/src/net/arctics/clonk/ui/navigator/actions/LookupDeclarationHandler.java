package net.arctics.clonk.ui.navigator.actions;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.actions.c4script.EntityChooser;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class LookupDeclarationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		ClonkProjectNature nat = ClonkProjectNature.get(part);
		List<Index> indices = new ArrayList<Index>();
		if (nat != null && nat.index() != null)
			indices.add(nat.index());
		else
			for (IProject p : ClonkProjectNature.clonkProjectsInWorkspace())
				indices.add(ClonkProjectNature.get(p).index());
		if (indices.size() > 0) {
			EntityChooser chooser = new EntityChooser(
				Platform.getResourceString(Core.instance().getBundle(), "%LookupDeclaration_Name"),
				HandlerUtil.getActiveShell(event), indices
			);
			chooser.run();
		}
		return null;
	}

}
