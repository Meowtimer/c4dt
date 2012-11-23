package net.arctics.clonk.debug;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;

public class SourceLookupDirector extends AbstractSourceLookupDirector {

	public SourceLookupDirector() {
	}

	@Override
	public void initializeParticipants() {
		this.addParticipants(new ISourceLookupParticipant[] {new SourceLookupParticipant()});
	}
	
	@Override
	public void initializeDefaults(ILaunchConfiguration configuration) throws CoreException {
		super.initializeDefaults(configuration);
		String projName = configuration.getAttribute(ClonkLaunchConfigurationDelegate.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
		if (proj != null) {
			this.setSourceContainers(new ISourceContainer[] {
					new ProjectSourceContainer(proj, true)
			});
		}
	}

}
