package net.arctics.clonk.builder;

import org.eclipse.core.runtime.jobs.Job;

public abstract class TypingMigrationJob extends Job {
	protected ClonkProjectNature nature;
	public TypingMigrationJob(String name, ClonkProjectNature nature) {
		super(name);
		this.nature = nature;
	}
}
