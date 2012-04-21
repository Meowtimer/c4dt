package net.arctics.clonk.ui.editors.actions.c4script;

import net.arctics.clonk.Core;
import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class LookupDeclarationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		ClonkProjectNature nat = ClonkProjectNature.get(part);
		if (nat != null && nat.index() != null) {
			EntityChooser chooser = new EntityChooser(
				Platform.getResourceString(Core.instance().getBundle(), "%LookupDeclaration_Name"),
				HandlerUtil.getActiveShell(event), nat.index()
			);
			chooser.run();
		}
		return null;
	}

}
