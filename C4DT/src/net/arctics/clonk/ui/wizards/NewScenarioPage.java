package net.arctics.clonk.ui.wizards;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class NewScenarioPage extends NewClonkFolderWizardPage {
	
	private Text titleText;

	public NewScenarioPage(ISelection selection) {
		super(selection);
		setTitle(Messages.NewScenarioPage_0);
		setDescription(Messages.NewScenarioPage_1);
		setFolderExtension(".c4s"); //$NON-NLS-1$
	}
	
	public String getTitle() {
		return titleText.getText();
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		fileText.setText(Messages.NewScenarioPage_3);
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		
		titleText = addTextField(Messages.NewScenarioPage_4, null);
		
		initialize();
		dialogChanged();
	}

}
