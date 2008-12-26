package net.arctics.clonk.ui.wizards;

import org.eclipse.jface.wizard.WizardPage;

public abstract class NewClonkFolderWizardPage extends WizardPage {

	protected NewClonkFolderWizardPage(String pageName) {
		super(pageName);
	}
	
	public abstract String getContainerName();
	public abstract String getFileName();

}
