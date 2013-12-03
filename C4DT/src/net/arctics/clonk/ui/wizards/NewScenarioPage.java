package net.arctics.clonk.ui.wizards;

import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.index.Engine;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class NewScenarioPage extends NewClonkFolderWizardPage {
	
	private Text titleText;

	public NewScenarioPage(final ISelection selection) {
		super(selection);
		final Engine engine = ClonkProjectNature.engineFromResource((IResource)((IStructuredSelection)selection).getFirstElement());
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
		folderText.setText(Messages.NewScenarioPage_FolderName);
	}
	
	@Override
	protected void layout(final Composite parent) {
		super.layout(parent);
		titleText = addTextField(Messages.NewScenarioPage_TitleText, null);
	}

}
