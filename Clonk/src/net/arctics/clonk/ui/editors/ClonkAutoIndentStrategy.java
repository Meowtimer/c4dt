package net.arctics.clonk.ui.editors;

import net.arctics.clonk.resource.ClonkProjectNature;

import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;

public class ClonkAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {
	private ClonkProjectNature project;
	private String partitioning;
	
	public ClonkAutoIndentStrategy(ClonkProjectNature project, String partitioning) {
		this.partitioning = partitioning;
		this.project = project;
	}
}
