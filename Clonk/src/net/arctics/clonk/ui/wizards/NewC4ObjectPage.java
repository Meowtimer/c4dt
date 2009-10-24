package net.arctics.clonk.ui.wizards;

import java.util.List;

import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class NewC4ObjectPage extends NewClonkFolderWizardPage {

	private Text c4idText;

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public NewC4ObjectPage(ISelection selection) {
		super(selection);
		setTitle(Messages.NewC4ObjectPage_0);
		setDescription(Messages.NewC4ObjectPage_1);
		this.selection = selection;
		setFolderExtension(".c4d"); //$NON-NLS-1$
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		c4idText = addTextField(Messages.NewC4ObjectPage_3);
		initialize();
		dialogChanged();
	}

	/**
	 * Ensures that both text fields are set.
	 */

	@Override
	protected void dialogChanged() {
		super.dialogChanged();
		if (c4idText.getText().length() != 4) {
			updateStatus(Messages.NewC4ObjectPage_4);
			return;
		}
		else {
			ClonkProjectNature nature = Utilities.getClonkNature(project);
			if (nature != null) {
				List<C4Object> objects = nature.getIndex().getObjects(C4ID.getID(c4idText.getText()));
				if (objects != null && !objects.isEmpty()) {
					updateStatus(Messages.NewC4ObjectPage_5);
					return;
				}
			}
		}
		updateStatus(null);
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		fileText.setText(Messages.NewC4ObjectPage_6);
	}
	
	public String getObjectID() {
		return c4idText.getText();
	}

}