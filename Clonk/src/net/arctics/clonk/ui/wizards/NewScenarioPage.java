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
		this.selection = selection;
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
		
		Composite container = (Composite) getControl();
		Label label = new Label(container, SWT.NULL);
		label.setText("");
		
		label = new Label(container, SWT.NULL);
		label.setText("&Scenario title:");
		titleText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		titleText.setLayoutData(gd);
		
		
		initialize();
		dialogChanged();
	}

}
