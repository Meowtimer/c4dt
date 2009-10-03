package net.arctics.clonk.resource.c4group;

import java.io.File;

import java.io.IOException;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.preferences.PreferenceConstants;
import net.arctics.clonk.util.FileOperations;
import net.arctics.clonk.util.Utilities;

import org.eclipse.swt.SWT;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class C4GroupExporter {

	private IContainer[] packs;
	private String[] destPaths;
	private String c4groupPath;
	private String destinationPath;

	public C4GroupExporter(IContainer[] packs, String c4groupPath, String destinatinoPath) {
		this.packs = packs;
		this.c4groupPath = c4groupPath;
		this.destinationPath = destinatinoPath;
	}

	public synchronized boolean selectDestPaths() {
		destPaths = new String[packs.length];
		int i = -1;
		FileDialog fileDialog = null;
		for (IResource pack : packs) {
			i++;
			if (!(pack instanceof IContainer))
				continue;
			IContainer toExport = (IContainer)pack;
			String packPath;
			if (!(pack.getParent() instanceof IProject)) {
				if (fileDialog == null)
					fileDialog = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
				fileDialog.setFileName(toExport.getName());
				fileDialog.setText("Where to save " + toExport.getName());
				fileDialog.setFilterPath(destinationPath);
				packPath = fileDialog.open();
				if (packPath == null) {
					destPaths = null;
					return false;
				}
			}
			else {
				packPath = new Path(destinationPath).append(toExport.getName()).toOSString();
			}
			destPaths[i] = packPath;
		}
		return destPaths != null;
	}

	public void export(IProgressMonitor monitor) {
		if (monitor != null)
			monitor.beginTask("Exporting", packs.length);
		IPreferencesService service = Platform.getPreferencesService();
		boolean showExportLog = service.getBoolean(ClonkCore.PLUGIN_ID, PreferenceConstants.SHOW_EXPORT_LOG, false, null);
		int i = -1;
		for(IResource pack : packs) {
			i++;
			if (!(pack instanceof IContainer))
				continue;
			IContainer toExport = (IContainer)pack;
			try {
				if (monitor != null)
					monitor.subTask(toExport.getName());
				String packPath = destPaths[i];
				File oldFile = new File(packPath);
				// ugh, deleting files is ugly but there seems to be no java method for putting files to trash -.-
				if (oldFile.exists())
					oldFile.delete();
				// copy directory to destination and pack it in-place
				FileOperations.copyDirectory(new File(packs[i].getRawLocation().toOSString()), oldFile);

				MessageConsoleStream out = null;
				if (showExportLog) {
					// get console
					MessageConsole myConsole = Utilities.getClonkConsole();
					out = myConsole.newMessageStream();
					Utilities.displayClonkConsole();
				}

				// create c4group command line
				String[] cmdArray = new String[] { c4groupPath, packPath, "-p" };
				if (showExportLog) {
					// show command line in console
					StringBuilder cmdLine = new StringBuilder();
					cmdLine.append("Command:");
					for (int _i = 0; _i < cmdArray.length; _i++) {
						String cmdE = cmdArray[_i];
						cmdLine.append(" " + cmdE);
					}
					out.println(cmdLine.toString());
				}

				// run c4group
				Process c4group = Runtime.getRuntime().exec(cmdArray, null, oldFile.getParentFile());
				if (showExportLog) {
					// pipe output to console
					java.io.InputStream stream = c4group.getInputStream();
					int read = 0;
					byte[] buffer = new byte[256];
					c4group.waitFor();

					while((read = stream.read(buffer, 0, 256)) > 0) {
						out.write(buffer, 0, read);
					}
				}
				oldFile = null;
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (monitor != null)
				monitor.worked(1);

			if (monitor != null)
				monitor.done();
		}
	}

}
