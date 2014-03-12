package net.arctics.clonk.ui.wizards;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;

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
		final Map<String, String> result = new HashMap<String, String>();
		result.put("$$Name$$", page.getFileName().substring(0, page.getFileName().lastIndexOf('.'))); //$NON-NLS-1$
		result.put("$$Author$$", ClonkPreferences.value(ClonkPreferences.AUTHOR));
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
		final IRunnableWithProgress op = monitor -> {
			try {
				doFinish(containerName, fileName, monitor);
			} catch (final CoreException e) {
				throw new InvocationTargetException(e);
			} finally {
				monitor.done();
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (final InterruptedException e) {
			return false;
		} catch (final InvocationTargetException e) {
			final Throwable realException = e.getTargetException();
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
		final String containerName,
		final String fileName,
		IProgressMonitor monitor)
		throws CoreException {

		monitor = new NullProgressMonitor(); // srsly, for creating some files...
		monitor.beginTask(String.format(Messages.NewClonkFolderWizard_CreatingFolder, fileName), 1);
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer))
			throwCoreException(String.format(Messages.NewClonkFolderWizard_FolderDoesNotExist, containerName));
		final IContainer container = (IContainer) resource;
		final IFolder newFolder = container.getFolder(new Path(fileName));
		if (!newFolder.exists())
			newFolder.create(IResource.NONE,true,monitor);
		try {
			final Iterable<URL> templates = getTemplateFiles();
			if (templates != null)
				for (final URL template : templates) {
					final String templateFile = new Path(template.getFile()).lastSegment();
					if (templateFile.startsWith(".")) //$NON-NLS-1$
						continue;
					final InputStream stream = getTemplateStream(template, templateFile);
					try {
						newFolder.getFile(templateFile).create(stream, true, monitor);
					} finally {
						stream.close();
					}
				}
			Display.getDefault().asyncExec(
				() -> UI.projectExplorer(workbenchWindow).selectReveal(new StructuredSelection(newFolder))
			);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	protected Iterable<URL> getTemplateFiles() {
		try {
			final ClonkProjectNature nature = ClonkProjectNature.get((IResource)((IStructuredSelection) selection).getFirstElement());
			return nature.index().engine().getURLsOfStorageLocationPath("wizards/"+getClass().getSimpleName(), false); //$NON-NLS-1$
		} catch (final Exception e) {
			return null;
		}
	}

	protected Map<String, String> getTemplateReplacements() {
		return templateReplacements;
	}

	protected InputStream getTemplateStream(final URL template, final String fileName) throws IOException {
		final InputStream result = template.openStream();
		if (fileName.endsWith(".txt") || fileName.endsWith(".c")) { //$NON-NLS-1$ //$NON-NLS-2$
			final Reader reader = new InputStreamReader(result);
			try {
				final StringBuilder builder = new StringBuilder();
				final char[] buffer = new char[1024];
				int read;
				while ((read = reader.read(buffer)) > 0)
					builder.append(buffer, 0, read);
				String readString = builder.toString();
				final Map<String, String> replacements = getTemplateReplacements();
				for (final Entry<String, String> entry : replacements.entrySet())
					readString = readString.replace(entry.getKey(), entry.getValue());
				return new ByteArrayInputStream(readString.getBytes());
			} finally {
				reader.close();
			}
		}
		return result;
	}

	protected void throwCoreException(final String message) throws CoreException {
		final IStatus status =
			new Status(IStatus.ERROR, Core.PLUGIN_ID, IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	@Override
	public void init(final IWorkbench workbench, final IStructuredSelection selection) {
		this.selection = selection;
		this.workbenchWindow = workbench.getActiveWorkbenchWindow();
	}
}