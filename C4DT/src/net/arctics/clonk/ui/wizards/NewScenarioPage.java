package net.arctics.clonk.ui.wizards;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class NewScenarioPage extends NewClonkFolderWizardPage {
	
	private Text titleText;

	public NewScenarioPage(ISelection selection) {
		super(selection);
		Engine engine = ClonkProjectNature.engineFromResource((IResource)((IStructuredSelection)selection).getFirstElement());
		setTitle(Messages.NewScenarioPage_Title);
		setDescription(Messages.NewScenarioPage_Description);
		setFolderExtension(engine.settings().groupTypeToFileExtensionMapping().get(GroupType.ScenarioGroup));
	}
	
	@Override
	protected GroupType groupType() {
		return GroupType.ScenarioGroup;
	}
	
	@Override
	public String getTitle() {
		return titleText.getText();
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		fileText.setText(Messages.NewScenarioPage_FolderName);
	}
	
	@Override
	protected void actuallyCreateControl(Composite parent) {
		super.actuallyCreateControl(parent);
		titleText = addTextField(Messages.NewScenarioPage_TitleText, null);
	}

}
