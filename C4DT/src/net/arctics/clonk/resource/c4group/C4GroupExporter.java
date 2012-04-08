package net.arctics.clonk.resource.c4group;

import java.io.File;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.FileOperations;
import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.Utilities;

import org.eclipse.swt.SWT;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class C4GroupExporter implements IRunnableWithProgress {
	
	private Map<Engine, List<Pair<IContainer, String>>> packsDividedInEngines = new HashMap<Engine, List<Pair<IContainer, String>>>();
	private String destinationPath;
	private int numTotal;
	
	private void divideInEngines(IContainer[] packs) {
		packsDividedInEngines.clear();
		for (IContainer c : packs) {
			if (c == null)
				continue;
			Engine engine = ClonkProjectNature.get(c).index().engine();
			List<Pair<IContainer, String>> list = packsDividedInEngines.get(engine);
			if (list == null) {
				list = new LinkedList<Pair<IContainer, String>>();
				packsDividedInEngines.put(engine, list);
			}
			list.add(new Pair<IContainer, String>(c, null));
		}
	}
	
	public C4GroupExporter(IContainer[] packs, String destinationPath) {
		divideInEngines(packs);
		this.numTotal = packs.length;
		this.destinationPath = destinationPath;
	}

	public synchronized boolean selectDestPaths() {
		for (Entry<Engine, List<Pair<IContainer, String>>> entry : packsDividedInEngines.entrySet()) {
			List<Pair<IContainer, String>> packs = entry.getValue();
			String destinationPath = this.destinationPath != null ? this.destinationPath : entry.getKey().settings().gamePath;
			FileDialog fileDialog = null;
			for (Pair<IContainer, String> toExport : packs) {
				String packPath;
				boolean alwaysAskForPath = true;
				if (alwaysAskForPath || !(toExport.first().getParent() instanceof IProject)) {
					if (fileDialog == null)
						fileDialog = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
					fileDialog.setFileName(toExport.first().getName());
					fileDialog.setText(String.format(Messages.WhereToSave, toExport.first().getName()));
					fileDialog.setFilterPath(destinationPath);
					packPath = fileDialog.open();
					if (packPath == null) {
						return false;
					}
				}
				else {
					packPath = new Path(destinationPath).append(toExport.first().getName()).toOSString();
				}
				toExport.setSecond(packPath);
			}
		}
		return true;
	}

	public void export(IProgressMonitor monitor) {
		if (monitor != null)
			monitor.beginTask(Messages.Exporting, numTotal);
		IPreferencesService service = Platform.getPreferencesService();
		final boolean showExportLog = service.getBoolean(Core.PLUGIN_ID, ClonkPreferences.SHOW_EXPORT_LOG, false, null);
		for (Entry<Engine, List<Pair<IContainer, String>>> byEngine : packsDividedInEngines.entrySet()) {
			final String c4groupPath = byEngine.getKey().settings().c4GroupPath;
			for(final Pair<IContainer, String> toExport : byEngine.getValue()) {
				if (monitor != null)
					monitor.subTask(toExport.first().getName());
				final String packPath = toExport.second();
				final File oldFile = new File(packPath);
				// ugh, deleting files is ugly but there seems to be no java method for putting files to trash -.-
				if (oldFile.exists())
					oldFile.delete();
				(new Job("Export C4Group") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							// copy directory to destination and pack it in-place
							FileOperations.copyDirectory(new File(toExport.first().getRawLocation().toOSString()), oldFile);
							
							// create c4group command line
							String[] cmdArray = new String[] { c4groupPath, packPath, "-p" }; //$NON-NLS-1$

							MessageConsoleStream out = null;
							if (showExportLog) {
								// get console
								MessageConsole myConsole = Utilities.clonkConsole();
								out = myConsole.newMessageStream();
								Utilities.displayClonkConsole();
								// show command line in console
								StringBuilder cmdLine = new StringBuilder();
								cmdLine.append(Messages.ExporterCommandlineTitle);
								for (int _i = 0; _i < cmdArray.length; _i++) {
									String cmdE = cmdArray[_i];
									cmdLine.append(" " + cmdE); //$NON-NLS-1$
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
							return Status.OK_STATUS;
						} catch (IOException e) {
							return Status.CANCEL_STATUS;
						} catch (InterruptedException e) {
							return Status.CANCEL_STATUS;
						}
					}
				}).schedule();
				if (monitor != null)
					monitor.worked(1);

				if (monitor != null)
					monitor.done();
			}
		}
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		export(monitor);
	}

}
