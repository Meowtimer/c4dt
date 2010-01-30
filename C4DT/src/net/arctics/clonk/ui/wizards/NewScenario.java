package net.arctics.clonk.ui.wizards;

import java.util.Map;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

/**
 * Wizard to create a new scenario
 */

public class NewScenario extends NewClonkFolderWizard {
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		setWindowTitle(Messages.NewScenario_Title);
	}
	@Override
	public void addPages() {
		page = new NewScenarioPage(selection);
		addPage(page);
	}
	@Override
	protected Map<String, String> initTemplateReplacements() {
		Map<String, String> result = super.initTemplateReplacements();
		result.put("$Title$", ((NewScenarioPage)page).getTitle()); //$NON-NLS-1$
		return result;
	}
}