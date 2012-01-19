package net.arctics.clonk.ui.wizards;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class NewDefinitionPage extends NewClonkFolderWizardPage {

	private Text c4idText;
	private Text descriptionText;

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public NewDefinitionPage(ISelection selection) {
		super(selection);
		setTitle(Messages.NewC4ObjectPage_Title);
		setDescription(Messages.NewC4ObjectPage_Description);
		this.selection = selection;
	}
	
	@Override
	protected GroupType groupType() {
		return GroupType.DefinitionGroup;
	}
	
	@Override
	protected void actuallyCreateControl(Composite parent) {
		super.actuallyCreateControl(parent);
		c4idText = addTextField(Messages.NewC4ObjectPage_ID);
		descriptionText = addTextField(Messages.NewC4ObjectPage_DescriptionLabel);
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
		ClonkProjectNature nature = ClonkProjectNature.get(project);
		if (nature != null) {
			if (!nature.getIndex().engine().acceptsId(c4idText.getText())) {
				updateStatus(Messages.NewC4ObjectPage_BadID);
				return;
			}
			Iterable<? extends Definition> objects = nature.getIndex().getDefinitionsWithID(ID.get(c4idText.getText()));
			if (objects != null) {
				updateStatus(Messages.NewC4ObjectPage_IDAlreadyInUse);
				return;
			}
		}
		updateStatus(null);
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		fileText.setText(Messages.NewC4ObjectPage_File);
		descriptionText.setText(Messages.NewC4ObjectPage_DescriptionDefault);
		setFolderExtension(ClonkProjectNature.getEngine(project).currentSettings().getGroupTypeToFileExtensionMapping().get(GroupType.DefinitionGroup));
	}
	
	public String getObjectID() {
		return c4idText.getText();
	}
	
	public String getObjectDescription() {
		return descriptionText.getText();
	}

}