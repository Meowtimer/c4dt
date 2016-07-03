package net.arctics.clonk.builder;

import org.eclipse.core.runtime.jobs.Job;

public abstract class TypingMigrationJob extends Job {
	protected ClonkProjectNature nature;
	public TypingMigrationJob(final String name, final ClonkProjectNature nature) {
		super(name);
		this.nature = nature;
	}
}
