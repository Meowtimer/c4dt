package net.arctics.clonk.ui.wizards;


import net.arctics.clonk.ClonkCore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import java.io.*;

import org.eclipse.ui.*;

/**
 * Base class for wizards creating all kinds of Clonk folders
 */

public class NewClonkFolderWizard extends Wizard implements INewWizard {
	protected NewClonkFolderWizardPage page;
	protected ISelection selection;
	private Map<String, String> templateReplacements; 

	/**
	 * Constructor for NewC4Object.
	 */
	public NewClonkFolderWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	protected Map<String, String> initTemplateReplacements() {
		Map<String, String> result = new HashMap<String, String>();
		result.put("$Name$", page.getFileName().substring(0, page.getFileName().lastIndexOf('.')));
		return result;
	}
	
	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		final String containerName = page.getContainerName();
		final String fileName = page.getFileName();
		templateReplacements = initTemplateReplacements();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(containerName, fileName, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */

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
				try {
					subContainer.getFile(templateFile).create(stream, true, monitor);
				} finally {
					stream.close();
				}
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
	
	@SuppressWarnings("unchecked")
	protected Enumeration<URL> getTemplateFiles() {
		return ClonkCore.getDefault().getBundle().findEntries("res/wizard/"+getClass().getSimpleName(), "*.*", false);
	}
	
	protected Map<String, String> getTemplateReplacements() {
		return templateReplacements;
	}
	
	protected InputStream getTemplateStream(URL template, String fileName) throws IOException {
		InputStream result = template.openStream();
		if (fileName.endsWith(".txt") || fileName.endsWith(".c")) {
			Reader reader = new InputStreamReader(result);
			StringBuilder builder = new StringBuilder();
			char[] buffer = new char[1024];
			int read;
			while ((read = reader.read(buffer)) > 0) {
				builder.append(buffer, 0, read);
			}
			String readString = builder.toString();
			Map<String, String> replacements = getTemplateReplacements();
			for (String key : replacements.keySet()) {
				String repl = replacements.get(key);
				readString = readString.replace(key, repl);
			}
			return new ByteArrayInputStream(readString.getBytes());
		}
		return template.openStream();
	}
	
	protected void throwCoreException(String message) throws CoreException {
		IStatus status =
			new Status(IStatus.ERROR, "net.arctics.clonk", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}