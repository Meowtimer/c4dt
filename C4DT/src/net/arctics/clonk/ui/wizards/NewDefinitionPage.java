package net.arctics.clonk.ui.wizards;

import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.C4Group.GroupType;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Text;

public class NewDefinitionPage extends NewClonkFolderWizardPage {

	private Text c4idText;
	private Text descriptionText;

	/**
	 * Constructor for SampleNewWizardPage.
	 *
	 * @param pageName
	 */
	public NewDefinitionPage(final ISelection selection) {
		super(selection);
		setTitle(Messages.NewC4ObjectPage_Title);
		setDescription(Messages.NewC4ObjectPage_Description);
		this.selection = selection;
	}

	@Override
	protected GroupType groupType() { return GroupType.DefinitionGroup; }

	@Override
	protected void fields() {
		c4idText = addTextField(Messages.NewC4ObjectPage_ID);
		c4idText.addModifyListener(e -> {
			if (folderText != null)
				folderText.setText(c4idText.getText());
		});
		descriptionText = addTextField(Messages.NewC4ObjectPage_DescriptionLabel);
		super.fields();
	}

	/**
	 * Ensures that both text fields are set.
	 */

//	public static boolean isValidC4ID(String c4id) {
//		if (c4id == null) return false;
//		//if (c4id.length() != 4) return false;
//		if (!c4id.matches("[A-Za-z_][A-Za-z_0-9]{3}")) return false; //$NON-NLS-1$
//		return true;
//	}

	@Override
	protected void dialogChanged() {
		super.dialogChanged();
		final ClonkProjectNature nature = ClonkProjectNature.get(project);
		if (nature != null)
			if (!nature.index().engine().acceptsId(c4idText.getText())) {
				updateStatus(Messages.NewC4ObjectPage_BadID);
				return;
			}
			/*Iterable<? extends Definition> objects = nature.index().definitionsWithID(ID.get(c4idText.getText()));
			if (objects != null) {
				updateStatus(Messages.NewC4ObjectPage_IDAlreadyInUse);
				return;
			}*/
		updateStatus(null);
	}

	@Override
	protected void initialize() {
		super.initialize();
		c4idText.setText(Messages.NewC4ObjectPage_File);
		folderText.setText(Messages.NewC4ObjectPage_File);
		descriptionText.setText(Messages.NewC4ObjectPage_DescriptionDefault);
		setFolderExtension(ClonkProjectNature.engineFromResource(project).settings().groupTypeToFileExtensionMapping().get(GroupType.DefinitionGroup));
	}

	public String objectID() {
		return c4idText.getText();
	}

	public String objectDescription() {
		return descriptionText.getText();
	}

}