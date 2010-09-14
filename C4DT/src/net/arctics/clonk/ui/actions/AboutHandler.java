package net.arctics.clonk.ui.actions;

import net.arctics.clonk.ClonkCore;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

public class AboutHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String version = (String) ClonkCore.getDefault().getBundle().getVersion().toString();
		String message = String.format(Messages.AboutHandler_InfoTemplate, version);
		MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.AboutHandler_Title, message);
		return null;
	}

}
