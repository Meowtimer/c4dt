package net.arctics.clonk.ui.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.runtime.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import java.io.*;

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

public class NewC4Object extends NewClonkFolderWizard implements INewWizard {

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
		result.put("$ID$", ((NewC4ObjectPage)page).getObjectID());
		result.put("$Name$", page.getFileName().substring(0, page.getFileName().lastIndexOf('.')));
		return result;
	}
	
	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */

	@Override
	protected void doFinish(
		String containerName,
		String fileName,
		IProgressMonitor monitor)
		throws CoreException {
		// create a sample file
		monitor.beginTask("Creating " + fileName, 1);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName + "\" does not exist.");
		}
		IContainer container = (IContainer) resource;
		final IFolder subContainer = container.getFolder(new Path(fileName));
		if (!subContainer.exists()) {
			subContainer.create(IResource.NONE,true,monitor);
		}
		try {
			Enumeration<URL> templates = getTemplateFiles();
			while (templates.hasMoreElements()) {
				URL template = templates.nextElement();
				String templateFile = new Path(template.getFile()).lastSegment();
				if (templateFile.startsWith("."))
					continue;
				InputStream stream = getTemplateStream(template, templateFile);
				subContainer.getFile(templateFile).create(stream, true, monitor);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		monitor.worked(1);
//		monitor.setTaskName("Opening file for editing...");
//		getShell().getDisplay().asyncExec(new Runnable() {
//			public void run() {
//				IWorkbenchPage page =
//					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
//				try {
//					IDE.openEditor(page, file, true);
//				} catch (PartInitException e) {
//				}
//			}
//		});
//		monitor.worked(1);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		setWindowTitle("Create new Object");
	}
}