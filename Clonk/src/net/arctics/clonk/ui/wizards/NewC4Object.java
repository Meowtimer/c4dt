package net.arctics.clonk.ui.wizards;


import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.C4ObjectIntern;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
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

public class NewC4Object extends Wizard implements INewWizard {
	private NewC4ObjectPage page;
	private ISelection selection;

	/**
	 * Constructor for NewC4Object.
	 */
	public NewC4Object() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new NewC4ObjectPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		final String containerName = page.getContainerName();
		final String fileName = page.getFileName();
		final String objectID = page.getObjectID().toUpperCase();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(containerName, fileName, objectID, monitor);
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

	private void doFinish(
		String containerName,
		String fileName,
		String objectID,
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
		new C4ObjectIntern(C4ID.getID(objectID), containerName, subContainer);
		try {
			InputStream stream;
			stream = initialDefCoreStream(fileName, objectID);
			subContainer.getFile("DefCore.txt").create(stream, true, monitor);
			stream.close();
			stream = initialDescDEStream();
			subContainer.getFile("DescDE.txt").create(stream, true, monitor);
			stream.close();
			stream = initialDescUSStream();
			subContainer.getFile("DescEN.txt").create(stream, true, monitor);
			stream.close();
			stream = initialNamesStream();
			subContainer.getFile("Names.txt").create(stream, true, monitor);
			stream.close();
			stream = initialGraphicsStream();
			subContainer.getFile("Graphics.png").create(stream, true, monitor);
			stream.close();
			stream = initialScriptStream();
			subContainer.getFile("Script.c").create(stream, true, monitor);
			stream.close();
		} catch (IOException e) {
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
	
	private InputStream initialDefCoreStream(String fileName, String objectID) {
		String contents =
			"[DefCore]" + "\r\n" +
			"id=" + objectID + "\r\n" +
			"Name=" + fileName.substring(0, fileName.lastIndexOf('.')) + "\r\n" +
			"Version=4,9,5" + "\r\n" +
			"Category=18960" + "\r\n" +
			"MaxUserSelect=10" + "\r\n" +
			"Width=6" + "\r\n" +
			"Height=6" + "\r\n" +
			"Offset=-3,-3" + "\r\n" +
			"Value=10" + "\r\n" +
			"Mass=10" + "\r\n" +
			"Components=" + objectID + "=1;" + "\r\n" +
			"Picture=6,0,64,64" + "\r\n" +
			"Vertices=1" + "\r\n" +
			"VertexY=1" + "\r\n" +
			"VertexFriction=20" + "\r\n" +
			"Rebuy=1" + "\r\n" +
			"Collectible=1";
		return new ByteArrayInputStream(contents.getBytes());
	}
	
	private InputStream initialDescDEStream() {
		String contents =
			"Eine neue Objektdefinition.";
		return new ByteArrayInputStream(contents.getBytes());
	}
	
	private InputStream initialDescUSStream() {
		String contents =
			"A new object definition.";
		return new ByteArrayInputStream(contents.getBytes());
	}
	
	private InputStream initialNamesStream() {
		String contents =
			"DE:Ein neues Objekt\r\nUS:A new object";
		return new ByteArrayInputStream(contents.getBytes());
	}
	
	private InputStream initialGraphicsStream() {
		try {
			return ClonkCore.getDefault().getBundle().getEntry("res/StdGraphics.png").openStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private InputStream initialScriptStream() {
		String contents =
			"/*-- Neues Objekt --*/\r\n\r\n#strict 2\r\n\r\nfunc Initialize() {\r\n  return(1);\r\n}";
		return new ByteArrayInputStream(contents.getBytes());
	}
	
	private void throwCoreException(String message) throws CoreException {
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
		setWindowTitle("Create new Object");
	}
}