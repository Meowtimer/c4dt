package net.arctics.clonk.ui.wizards;

import java.util.Map;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;

public class NewParticle extends NewClonkFolderWizard {
	
	public String title;
	
	@Override
	public void addPages() {
		page = new NewClonkFolderWizardPage(selection);
		page.setFolderExtension(".c4d"); //$NON-NLS-1$
		page.setTitle(Messages.NewParticle_1);
		page.setDescription(Messages.NewParticle_2);
		addPage(page);
	}
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		setWindowTitle(Messages.NewParticle_3);
	}
	@Override
	protected Map<String, String> initTemplateReplacements() {
		Map<String, String> result = super.initTemplateReplacements();
		result.put("$Title$", title); //$NON-NLS-1$
		return result;
	}
	@Override
	public void createPageControls(Composite pageContainer) {		
		super.createPageControls(pageContainer);
		page.addTextField(Messages.NewParticle_5, this, "title", null); //$NON-NLS-2$ //$NON-NLS-1$
		page.getFileText().setText(Messages.NewParticle_7);
	}
	
}
