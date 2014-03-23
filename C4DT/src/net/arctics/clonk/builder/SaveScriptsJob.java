package net.arctics.clonk.builder;

import java.io.IOException;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.Index;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

class SaveScriptsJob extends Job {
	private final Set<Script> scriptsToSave;
	private final IProject project;
	public SaveScriptsJob(final IProject project, final Script[] scriptsToSave) {
		super(ClonkBuilder.buildTask(Messages.ClonkBuilder_SaveIndexFilesForParsedScripts, project));
		this.scriptsToSave = new HashSet<>(Arrays.asList(scriptsToSave));
		this.project = project;
	}
	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		final Set<Index> indices = scriptsToSave.stream().map(s -> s.index()).collect(Collectors.toSet());
		monitor.beginTask(ClonkBuilder.buildTask(Messages.ClonkBuilder_SavingScriptIndexFiles, project), scriptsToSave.size()+3+indices.size());
		try {
			for (final Iterator<Script> it = scriptsToSave.iterator(); it.hasNext();) {
				final Script s = it.next();
				try {
					s.save();
					it.remove();
					monitor.worked(1);
				} catch (final ConcurrentModificationException mod) {
					System.out.println(
						String.format("Failed to save %s due to modification during save() - retrying in hopes of modifications settling down ",
							s.qualifiedName()));
				} catch (final IOException e) {
					System.out.println(String.format("%s could not be saved due to IO: %s",
						s.qualifiedName(), e.getMessage()));
				}
			}
			if (scriptsToSave.size() > 0) {
				System.out.println("Keep SaveScriptsJob running");
				schedule(1500);
			}
			indices.forEach(index -> {
				for (int attempt = 0; attempt < 3; attempt++)
					try {
						index.saveShallow();
						monitor.worked(1);
						break;
					} catch (final Exception e) {}
			});
			monitor.worked(3);
			return Status.OK_STATUS;
		} finally {
			monitor.done();
		}
	}
}