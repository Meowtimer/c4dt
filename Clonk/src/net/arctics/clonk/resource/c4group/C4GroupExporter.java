package net.arctics.clonk.resource.c4group;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

public class C4GroupExporter {
	
	private IContainer[] definitions;
	private String c4groupPath;
	private String destinationPath;
	
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
				if (monitor != null) monitor.subTask(toExport.getName());
				String c4dpath = new Path(destinationPath).append(toExport.getName()).toOSString();
				File oldFile = new File(new Path(destinationPath).append(toExport.getName()).toOSString());
				if (oldFile.exists()) oldFile.delete();
				//String[] cmdArray = new String[] { c4groupPath, c4dpath, "/r -a " + toExport.getLocation().append("*").toOSString() };
				String cmd = c4groupPath + " " + c4dpath + " /r -a " + toExport.getLocation().append("*").toOSString();
				System.out.println(cmd);
				Process c4group = Runtime.getRuntime().exec(cmd, null, oldFile.getParentFile());
				
				//Process c4group = Runtime.getRuntime().exec(cmdArray, new String[0], oldFile.getParentFile());
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(c4group.getInputStream()));
				String line = null;
				while((line = reader.readLine()) != null) {
					System.out.println(line);
				}
				c4group.waitFor();
				oldFile = null;
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
