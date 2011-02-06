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

public class NewC4Object extends NewClonkFolderWizard<NewC4ObjectPage> implements INewWizard {

	/**
	 * Constructor for NewC4Object.
	 */
	public NewC4Object() {
		super();
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new NewC4ObjectPage(selection);
		addPage(page);
	}

	protected Map<String, String> initTemplateReplacements() {
		Map<String, String> result = super.initTemplateReplacements();
		result.put("$$ID$$", page.getObjectID()); //$NON-NLS-1$
		result.put("$$Description$$", page.getObjectDescription()); //$NON-NLS-1$
		return result;
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		setWindowTitle(Messages.NewC4Object_Title);
	}
}