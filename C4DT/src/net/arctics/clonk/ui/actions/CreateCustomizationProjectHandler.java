package net.arctics.clonk.ui.actions;

import net.arctics.clonk.resource.CustomizationNature;
import net.arctics.clonk.util.UI;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.PlatformUI;

public class CreateCustomizationProjectHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		CustomizationNature custom = CustomizationNature.get();
		if (custom != null)
			Utilities.getProjectExplorer().selectReveal(new StructuredSelection(custom.getProject()));
		else {
			String val = UI.input(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.CreateCustomizationProjectHandler_ProvideName, Messages.CreateCustomizationProjectHandler_NamePromptDesc, Messages.CreateCustomizationProjectHandler_DefaultName);
			if (val != null)
				CustomizationNature.create(val);
		}
		return null;
	}

}
