package net.arctics.clonk.c4group;

import static java.util.Arrays.stream;
import static net.arctics.clonk.util.Utilities.bucketize;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import net.arctics.clonk.Core;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.Console;
import net.arctics.clonk.util.FileOperations;
import net.arctics.clonk.util.Pair;

public class C4GroupExporter implements IRunnableWithProgress {
	
	private final Map<Engine, List<Pair<IContainer, String>>> packsDividedInEngines;
	private final String destinationPath;
	private final int numTotal;
	
	private static Map<Engine, List<Pair<IContainer, String>>> divideInEngines(final IContainer[] packs) {
		return bucketize(
			stream(packs)
				.filter(c -> c != null)
				.map(c -> new Pair<IContainer, String>(c, null)),
			c -> ClonkProjectNature.get(c.first()).index().engine()
		);
	}
	
	public C4GroupExporter(final IContainer[] packs, final String destinationPath) {
		this.packsDividedInEngines = divideInEngines(packs);
		this.destinationPath = destinationPath;
		this.numTotal = packs.length;
	}

	public synchronized boolean selectDestPaths() {
		for (final Entry<Engine, List<Pair<IContainer, String>>> entry : packsDividedInEngines.entrySet()) {
			final List<Pair<IContainer, String>> packs = entry.getValue();
			final String destinationPath = this.destinationPath != null ? this.destinationPath : entry.getKey().settings().gamePath;
			FileDialog fileDialog = null;
			for (final Pair<IContainer, String> toExport : packs) {
				String packPath;
				if (fileDialog == null) {
					fileDialog = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
				}
				fileDialog.setFileName(toExport.first().getName());
				fileDialog.setText(String.format(Messages.WhereToSave, toExport.first().getName()));
				fileDialog.setFilterPath(destinationPath);
				packPath = fileDialog.open();
				if (packPath == null) {
					return false;
				}
				toExport.setSecond(packPath);
			}
		}
		return true;
	}

	public void export(final IProgressMonitor monitor) {
		if (monitor != null) {
			monitor.beginTask(Messages.Exporting, numTotal);
		}
		final IPreferencesService service = Platform.getPreferencesService();
		final boolean showExportLog = service.getBoolean(Core.PLUGIN_ID, ClonkPreferences.SHOW_EXPORT_LOG, false, null);
		for (final Entry<Engine, List<Pair<IContainer, String>>> byEngine : packsDividedInEngines.entrySet()) {
			final String c4groupPath = byEngine.getKey().settings().c4GroupPath;
			for(final Pair<IContainer, String> toExport : byEngine.getValue()) {
				if (monitor != null) {
					monitor.subTask(toExport.first().getName());
				}
				final String packPath = toExport.second();
				final File oldFile = new File(packPath);
				// ugh, deleting files is ugly but there seems to be no java method for putting files to trash -.-
				if (oldFile.exists()) {
					oldFile.delete();
				}
				new Job(String.format(Messages.ExportC4GroupJobTitle, toExport.first().getName())) {
					@Override
					protected IStatus run(final IProgressMonitor monitor) {
						try {
							// copy directory to destination and pack it in-place
							FileOperations.copyDirectory(new File(toExport.first().getRawLocation().toOSString()), oldFile);
							
							// create c4group command line
							final String[] cmdArray = new String[] { c4groupPath, packPath, "-p" }; //$NON-NLS-1$

							MessageConsoleStream out = null;
							if (showExportLog) {
								// get console
								final MessageConsole myConsole = Console.clonkConsole();
								out = myConsole.newMessageStream();
								Console.display();
								// show command line in console
								final StringBuilder cmdLine = new StringBuilder();
								cmdLine.append(Messages.ExporterCommandlineTitle);
								for (final String cmdE : cmdArray) {
									cmdLine.append(" " + cmdE); //$NON-NLS-1$
								}
								out.println(cmdLine.toString());
							}

							// run c4group
							final Process c4group = Runtime.getRuntime().exec(cmdArray, null, oldFile.getParentFile());
							if (showExportLog) {
								// pipe output to console
								final java.io.InputStream stream = c4group.getInputStream();
								int read = 0;
								final byte[] buffer = new byte[256];
								c4group.waitFor();

								while((read = stream.read(buffer, 0, 256)) > 0) {
									out.write(buffer, 0, read);
								}
							}
							c4group.waitFor();
							return Status.OK_STATUS;
						} catch (final IOException e) {
							e.printStackTrace();
							return Status.CANCEL_STATUS;
						} catch (final InterruptedException e) {
							e.printStackTrace();
							return Status.CANCEL_STATUS;
						}
					}
				}.schedule();
				if (monitor != null) {
					monitor.worked(1);
				}
			}
		}
		if (monitor != null) {
			monitor.done();
		}
	}

	@Override
	public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		export(monitor);
	}

}
