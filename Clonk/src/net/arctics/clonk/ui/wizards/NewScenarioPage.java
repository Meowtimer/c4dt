package net.arctics.clonk.ui.wizards;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NewScenarioPage extends NewClonkFolderWizardPage {
	
	private Text titleText;

	public NewScenarioPage(ISelection selection) {
		super(selection);
		setTitle("Create new scenario");
		setDescription("This wizard creates a new scenario folder");
		setFolderExtension(".c4s");
	}
	
	public String getTitle() {
		return titleText.getText();
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		fileText.setText("NewScenario");
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		
		titleText = addTextField("&Scenario title:", null);
		
		initialize();
		dialogChanged();
	}

}
