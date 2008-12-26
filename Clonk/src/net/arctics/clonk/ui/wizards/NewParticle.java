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
		page.setFolderExtension(".c4d");
		page.setTitle("Create new particle");
		page.setDescription("This wizard creates a new particle");
		addPage(page);
	}
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		setWindowTitle("New Particle");
	}
	@Override
	protected Map<String, String> initTemplateReplacements() {
		Map<String, String> result = super.initTemplateReplacements();
		result.put("$Title$", title);
		return result;
	}
	@Override
	public void createPageControls(Composite pageContainer) {		
		super.createPageControls(pageContainer);
		page.addTextField("&Title:", this, "title", null);
		page.getFileText().setText("NewParticle");
	}
	
}
