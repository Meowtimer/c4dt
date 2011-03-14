package net.arctics.clonk.ui.wizards;


import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import java.io.*;

import org.eclipse.ui.*;

/**
 * Base class for wizards creating all kinds of Clonk folders
 */

public abstract class NewClonkFolderWizard<PageClass extends NewClonkFolderWizardPage> extends Wizard implements INewWizard {
	protected PageClass page;
	protected ISelection selection;
	private Map<String, String> templateReplacements;
	
	private IWorkbenchWindow workbenchWindow;

	/**
	 * Constructor for NewC4Object.
	 */
	public NewClonkFolderWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	protected Map<String, String> initTemplateReplacements() {
		Map<String, String> result = new HashMap<String, String>();
		result.put("$$Name$$", page.getFileName().substring(0, page.getFileName().lastIndexOf('.'))); //$NON-NLS-1$
		result.put("$$Author$$", ClonkPreferences.getPreferenceOrDefault(ClonkPreferences.AUTHOR));
		return result;
	}
	
	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	@Override
	public boolean performFinish() {
		final String containerName = page.getContainerName();
		final String fileName = page.getFileName();
		templateReplacements = initTemplateReplacements();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			@Override
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
			MessageDialog.openError(getShell(), Messages.NewClonkFolderWizard_Error, realException.getMessage());
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

		monitor = new NullProgressMonitor(); // srsly, for creating some files...
		monitor.beginTask(String.format(Messages.NewClonkFolderWizard_CreatingFolder, fileName), 1);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException(String.format(Messages.NewClonkFolderWizard_FolderDoesNotExist, containerName));
		}
		IContainer container = (IContainer) resource;
		final IFolder newFolder = container.getFolder(new Path(fileName));
		if (!newFolder.exists()) {
			newFolder.create(IResource.NONE,true,monitor);
		}
		try {
			Iterable<URL> templates = getTemplateFiles();
			if (templates != null) {
				for (URL template : templates) {
					String templateFile = new Path(template.getFile()).lastSegment();
					if (templateFile.startsWith(".")) //$NON-NLS-1$
						continue;
					InputStream stream = getTemplateStream(template, templateFile);
					try {
						newFolder.getFile(templateFile).create(stream, true, monitor);
					} finally {
						stream.close();
					}
				}
			}
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					Utilities.getProjectExplorer(workbenchWindow).selectReveal(new StructuredSelection(newFolder));
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected Iterable<URL> getTemplateFiles() {
		try {
			ClonkProjectNature nature = ClonkProjectNature.get((IResource)((IStructuredSelection) selection).getFirstElement());
			return nature.getIndex().getEngine().getURLsOfStorageLocationPath("wizards/"+getClass().getSimpleName(), false); //$NON-NLS-1$
		} catch (Exception e) {
			return null;
		}
	}
	
	protected Map<String, String> getTemplateReplacements() {
		return templateReplacements;
	}
	
	protected InputStream getTemplateStream(URL template, String fileName) throws IOException {
		InputStream result = template.openStream();
		if (fileName.endsWith(".txt") || fileName.endsWith(".c")) { //$NON-NLS-1$ //$NON-NLS-2$
			Reader reader = new InputStreamReader(result);
			try {
				StringBuilder builder = new StringBuilder();
				char[] buffer = new char[1024];
				int read;
				while ((read = reader.read(buffer)) > 0) {
					builder.append(buffer, 0, read);
				}
				String readString = builder.toString();
				Map<String, String> replacements = getTemplateReplacements();
				for (Entry<String, String> entry : replacements.entrySet()) {
					readString = readString.replace(entry.getKey(), entry.getValue());
				}
				return new ByteArrayInputStream(readString.getBytes());
			} finally {
				reader.close();
			}
		}
		return result;
	}
	
	protected void throwCoreException(String message) throws CoreException {
		IStatus status =
			new Status(IStatus.ERROR, ClonkCore.PLUGIN_ID, IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
		this.workbenchWindow = workbench.getActiveWorkbenchWindow();
	}
}