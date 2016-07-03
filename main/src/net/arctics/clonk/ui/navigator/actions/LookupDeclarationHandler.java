package net.arctics.clonk.ui.navigator.actions;

import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.ui.editors.actions.c4script.EntityChooser;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.handlers.HandlerUtil;

public class LookupDeclarationHandler extends AbstractHandler {
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final Set<Declaration> declarations = new HashSet<Declaration>();
		for (final IProject p : ClonkProjectNature.clonkProjectsInWorkspace()) {
			declarations.add(ClonkProjectNature.get(p).index());
			declarations.add(ClonkProjectNature.get(p).index().engine());
		}
		if (declarations.size() > 0) {
			final EntityChooser chooser = new EntityChooser(
				Platform.getResourceString(Core.instance().getBundle(), "%LookupDeclaration_Name"),
				HandlerUtil.getActiveShell(event), declarations
			);
			chooser.run();
		}
		return null;
	}
}
