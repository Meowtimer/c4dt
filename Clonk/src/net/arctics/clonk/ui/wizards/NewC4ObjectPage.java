package net.arctics.clonk.ui.wizards;

import java.util.List;

import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (mpe).
 */

public class NewC4ObjectPage extends NewClonkFolderWizardPage {
	
	private IProject project;
	private Text c4idText;

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public NewC4ObjectPage(ISelection selection) {
		super(selection);
		setTitle("Create new object definition");
		setDescription("This wizard creates a new object definition structure");
		this.selection = selection;
		setFolderExtension(".c4d");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		c4idText = addTextField("Object &ID:");
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
			updateStatus("Object ID must be valid");
			return;
		}
		else {
			try {
				ClonkProjectNature nature = (ClonkProjectNature)project.getNature("net.arctics.clonk.clonknature");
				if (nature != null) {
					List<C4Object> objects = nature.getIndexedData().getObjects(C4ID.getID(c4idText.getText()));
					if (objects != null && !objects.isEmpty()) {
						updateStatus("Object ID is already in use.");
						return;
					}
				}
			} catch (CoreException e) {
				// ignore
			}
		}
		updateStatus(null);
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		fileText.setText("NewObject");
	}
	
	public String getObjectID() {
		return c4idText.getText();
	}

}