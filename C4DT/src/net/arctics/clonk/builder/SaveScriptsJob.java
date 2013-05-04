package net.arctics.clonk.builder;

import java.io.IOException;

import net.arctics.clonk.c4script.Script;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

class SaveScriptsJob extends Job {
	private final Script[] scriptsToSave;
	private final IProject project;
	public SaveScriptsJob(IProject project, Script[] scriptsToSave) {
		super(ClonkBuilder.buildTask(Messages.ClonkBuilder_SaveIndexFilesForParsedScripts, project));
		this.scriptsToSave = scriptsToSave;
		this.project = project;
	}
	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		monitor.beginTask(ClonkBuilder.buildTask(Messages.ClonkBuilder_SavingScriptIndexFiles, project), scriptsToSave.length+3);
		try {
			for (final Script s : scriptsToSave)
				try {
					s.save();
					monitor.worked(1);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			monitor.worked(3);
			return Status.OK_STATUS;
		} finally {
			monitor.done();
		}
	}
}