package net.arctics.clonk.ui.wizards;

import java.util.List;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
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
		Engine engine = ClonkProjectNature.getEngine(selection);
		if (engine != null)
			setImageDescriptor(engine.imageDescriptor(GroupType.DefinitionGroup.name()+"Big"));
		this.selection = selection;
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
			if (!nature.getIndex().getEngine().acceptsId(c4idText.getText())) {
				updateStatus(Messages.NewC4ObjectPage_BadID);
				return;
			}
			List<Definition> objects = nature.getIndex().getDefinitionsWithID(ID.getID(c4idText.getText()));
			if (objects != null && !objects.isEmpty()) {
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
		setFolderExtension(ClonkProjectNature.getEngine(project).getCurrentSettings().getGroupTypeToFileExtensionMapping().get(GroupType.DefinitionGroup));
	}
	
	public String getObjectID() {
		return c4idText.getText();
	}
	
	public String getObjectDescription() {
		return descriptionText.getText();
	}

}