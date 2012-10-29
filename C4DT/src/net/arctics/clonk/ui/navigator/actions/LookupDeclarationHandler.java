package net.arctics.clonk.ui.navigator.actions;

import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;
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
		Set<Declaration> declarations = new HashSet<Declaration>();
		if (nat != null && nat.index() != null) {
			declarations.add(nat.index());
			declarations.add(nat.index().engine());
		}
		else
			for (IProject p : ClonkProjectNature.clonkProjectsInWorkspace()) {
				declarations.add(ClonkProjectNature.get(p).index());
				declarations.add(ClonkProjectNature.get(p).index().engine());
			}
		if (declarations.size() > 0) {
			EntityChooser chooser = new EntityChooser(
				Platform.getResourceString(Core.instance().getBundle(), "%LookupDeclaration_Name"),
				HandlerUtil.getActiveShell(event), declarations
			);
			chooser.run();
		}
		return null;
	}

}
