package net.arctics.clonk.ui.search;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

public class C4ScriptSearchPage extends DialogPage implements ISearchPage {
	private Text templateText;
	private Text replacementText;
	private ISearchPageContainer container;

	/**
	 * @wbp.parser.constructor
	 */
	public C4ScriptSearchPage() {}

	public C4ScriptSearchPage(String title) {
		super(title);
	}

	public C4ScriptSearchPage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void createControl(Composite parent) {
		Composite ctrl = new Composite(parent, SWT.NONE);
		setControl(ctrl);
		GridLayout gl_ctrl = new GridLayout(2, false);
		ctrl.setLayout(gl_ctrl);
		
		Label presetLabel = new Label(ctrl, SWT.NONE);
		presetLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		presetLabel.setText("Template Preset");
		
		Combo presetCombo = new Combo(ctrl, SWT.READ_ONLY);
		GridData gd_presetCombo = new GridData(GridData.FILL_HORIZONTAL);
		gd_presetCombo.widthHint = 568;
		presetCombo.setLayoutData(gd_presetCombo);
		
		Label customTemplateLabel = new Label(ctrl, SWT.NONE);
		customTemplateLabel.setText("Custom Template");
		customTemplateLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		
		templateText = new Text(ctrl, SWT.BORDER);
		GridData gd_templateText = new GridData(GridData.FILL_BOTH);
		gd_templateText.widthHint = 527;
		gd_templateText.heightHint = 150;
		templateText.setLayoutData(gd_templateText);
		
		Label replacementLabel = new Label(ctrl, SWT.NONE);
		replacementLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		replacementLabel.setText("Replacement");
		
		replacementText = new Text(ctrl, SWT.BORDER);
		GridData gd_replacementText = new GridData(GridData.FILL_BOTH);
		gd_replacementText.heightHint = 168;
		replacementText.setLayoutData(gd_replacementText);
	}

	@Override
	public boolean performAction() {
		
		return true;
	}

	@Override
	public void setContainer(ISearchPageContainer container) {
		this.container = container;
	}
}
