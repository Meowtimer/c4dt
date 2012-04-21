package net.arctics.clonk.ui.editors.actions.c4script;

import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class LookupDeclarationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		ClonkProjectNature nat = ClonkProjectNature.get(part);
		if (nat != null && nat.index() != null)
			new EntityChooser(HandlerUtil.getActiveShell(event), nat.index()).run();
		return null;
	}

}
