package net.arctics.clonk.ui.wizards;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class NewScenarioPage extends NewClonkFolderWizardPage {
	
	private Text titleText;

	public NewScenarioPage(ISelection selection) {
		super(selection);
		setTitle(Messages.NewScenarioPage_Title);
		setDescription(Messages.NewScenarioPage_Description);
		setFolderExtension(".c4s"); //$NON-NLS-1$
	}
	
	public String getTitle() {
		return titleText.getText();
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		fileText.setText(Messages.NewScenarioPage_FolderName);
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		
		titleText = addTextField(Messages.NewScenarioPage_TitleText, null);
		
		initialize();
		dialogChanged();
	}

}
