package net.arctics.clonk.resource.c4group;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.preferences.PreferenceConstants;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class C4GroupExporter {
	
	private IContainer[] definitions;
	private String c4groupPath;
	private String destinationPath;
	
	private static File scratchExecFile;
	
	public C4GroupExporter(IContainer[] definitions, String c4groupPath, String destinatinoPath) {
		this.definitions = definitions;
		this.c4groupPath = c4groupPath;
		this.destinationPath = destinatinoPath;
	}
	
	public void export(IProgressMonitor monitor) {
		if (monitor != null) monitor.beginTask("Export definitions", definitions.length);
		IPreferencesService service = Platform.getPreferencesService();
		boolean showExportLog = service.getBoolean(ClonkCore.PLUGIN_ID, PreferenceConstants.SHOW_EXPORT_LOG, false, null);
		
		for(IResource res : definitions) {
			if (!(res instanceof IContainer)) continue;
			IContainer toExport = (IContainer)res;
			try {
				String OS = System.getProperty("os.name");
				if (monitor != null) monitor.subTask(toExport.getName());
				String c4dpath = new Path(destinationPath).append(toExport.getName()).toOSString();
				File oldFile = new File(new Path(destinationPath).append(toExport.getName()).toOSString());
				if (oldFile.exists()) oldFile.delete();
				
				// ugly hack :S create temporary file that uses /bin/sh to execute c4group 
				if (OS.equals("Mac OS X")) {
					if (scratchExecFile == null) {
						// create and make executable
						scratchExecFile = File.createTempFile("c4groupproxy", "eclipse");
						scratchExecFile.deleteOnExit();
						Runtime.getRuntime().exec(new String[] {"/bin/chmod", "+x", scratchExecFile.getAbsolutePath()}).waitFor();
					}
					// write command
					Writer writer = new OutputStreamWriter(new FileOutputStream(scratchExecFile));
					writer.write("sh -c \"" + c4groupPath + " $1 /r -a $2/*\"");
					writer.close();
					// exec file with destination and source as parameters
					Runtime.getRuntime().exec(new String[] {scratchExecFile.getAbsolutePath(), c4dpath, toExport.getLocation().toOSString()}).waitFor();
				}
				else {
					MessageConsoleStream out = null;
					if (showExportLog) {
						// get console
						MessageConsole myConsole = findConsole("Clonk");
						out = myConsole.newMessageStream();
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						String id = IConsoleConstants.ID_CONSOLE_VIEW;
						
						// show console 
						try {
							IConsoleView view = (IConsoleView) page.showView(id);
							view.display(myConsole);
						} catch (PartInitException e) {
							e.printStackTrace();
						}
					}
					
					// create c4group command line
					String[] cmdArray = new String[] { c4groupPath, c4dpath, "/r", "-a", new Path(toExport.getLocation().toString()).append("*").toOSString() };
//					String cmd = "\"" + c4groupPath + "\" \"" + c4dpath + "\" /r -a \"" + new Path(toExport.getLocation().toString()).append("*").toOSString() + "\"";
//					System.out.println(cmd);
					
					if (showExportLog) {
						// show command line in console
						StringBuilder cmdLine = new StringBuilder();
						cmdLine.append("Command:");
						for(String cmdE : cmdArray) cmdLine.append(" " + cmdE);
						out.println(cmdLine.toString());
					}
					
					// run c4group
					Process c4group = Runtime.getRuntime().exec(cmdArray, new String[0], oldFile.getParentFile());
					
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
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (monitor != null) monitor.worked(1);
		}
		if (monitor != null) monitor.done();
	}

	private MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		//no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[]{myConsole});
		return myConsole;
	}
}
