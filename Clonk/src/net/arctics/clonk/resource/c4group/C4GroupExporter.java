package net.arctics.clonk.resource.c4group;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

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
					String[] cmdArray = new String[] { c4groupPath, c4dpath, "/r", "-a", new Path(toExport.getLocation().toString()).append("*").toOSString() };
//					String cmd = "\"" + c4groupPath + "\" \"" + c4dpath + "\" /r -a \"" + new Path(toExport.getLocation().toString()).append("*").toOSString() + "\"";
//					System.out.println(cmd);
					Process c4group = Runtime.getRuntime().exec(cmdArray, new String[0], oldFile.getParentFile());
					c4group.waitFor();
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
}
