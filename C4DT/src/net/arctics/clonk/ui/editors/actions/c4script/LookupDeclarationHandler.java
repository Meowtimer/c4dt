package net.arctics.clonk.ui.editors.actions.c4script;

import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class LookupDeclarationHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ClonkProjectNature nat = ClonkProjectNature.get(HandlerUtil.getActiveSite(event));
		if (nat != null && nat.getIndex() != null)
			new DeclarationChooser(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), nat.getIndex()).run();
		return null;
	}

}
