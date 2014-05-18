package net.arctics.clonk.ui.wizards;

import java.util.Map;

import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.FileExtension;
import net.arctics.clonk.util.UI;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;

public class NewParticle extends NewClonkFolderWizard<NewClonkFolderWizardPage> {
	
	public String title;
	
	@Override
	public void addPages() {
		page = new NewClonkFolderWizardPage(selection) {
			@Override
			protected void initialize() {
				super.initialize();
				setFolderExtension(ClonkProjectNature.engineFromResource(project).settings().groupTypeToFileExtensionMapping().get(FileExtension.DefinitionGroup));
				setTitle(Messages.NewParticle_PageTitle);
				setDescription(Messages.NewParticle_Description);
				setImageDescriptor(UI.imageDescriptorForPath("icons/particlebig.png"));
			};
		};
		addPage(page);
	}
	@Override
	public void init(final IWorkbench workbench, final IStructuredSelection selection) {
		super.init(workbench, selection);
		setWindowTitle(Messages.NewParticle_Title);
	}
	@Override
	protected Map<String, String> initTemplateReplacements() {
		final Map<String, String> result = super.initTemplateReplacements();
		result.put("$$Title$$", title); //$NON-NLS-1$
		return result;
	}
	@Override
	public void createPageControls(final Composite pageContainer) {		
		super.createPageControls(pageContainer);
		page.addTextField(Messages.NewParticle_TitleText, this, "title", null); //$NON-NLS-1$
		page.getFileText().setText(Messages.NewParticle_FolderName);
	}
	
}
