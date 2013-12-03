package net.arctics.clonk.ui.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import java.util.Map;
import org.eclipse.ui.*;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "mpe". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */

public class NewDefinition extends NewClonkFolderWizard<NewDefinitionPage> implements INewWizard {

	/**
	 * Constructor for NewDefinitionWizard.
	 */
	public NewDefinition() {
		super();
	}
	
	/**
	 * Adding the page to the wizard.
	 */
	@Override
	public void addPages() {
		page = new NewDefinitionPage(selection);
		addPage(page);
	}

	@Override
	protected Map<String, String> initTemplateReplacements() {
		final Map<String, String> result = super.initTemplateReplacements();
		result.put("$$ID$$", page.objectID()); //$NON-NLS-1$
		result.put("$$Description$$", page.objectDescription()); //$NON-NLS-1$
		return result;
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	@Override
	public void init(final IWorkbench workbench, final IStructuredSelection selection) {
		super.init(workbench, selection);
		setWindowTitle(Messages.NewC4Object_Title);
	}
}