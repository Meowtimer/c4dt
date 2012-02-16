package net.arctics.clonk.debug;

import net.arctics.clonk.Core;
import net.arctics.clonk.ui.debug.ClonkDebugModelPresentation;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.LineBreakpoint;

public class ClonkDebugLineBreakpoint extends LineBreakpoint {

	public static final String ID = ClonkDebugLineBreakpoint.class.getName(); // who needs ids differing from class name -.-

	public ClonkDebugLineBreakpoint() {
		super();
	}
	
	public ClonkDebugLineBreakpoint(final IResource resource, final int lineNumber) throws CoreException {
		IWorkspaceRunnable markerAttribs = new IWorkspaceRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				IMarker marker = resource.createMarker(Core.id("breakpointMarker")); //$NON-NLS-1$
				marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
				marker.setAttribute(IBreakpoint.ENABLED, true);
				marker.setAttribute(IBreakpoint.ID, getModelIdentifier());
				marker.setAttribute(IMarker.MESSAGE, String.format(Messages.ClonkDebugLineBreakpoint_BreakpointMessage, resource.getProjectRelativePath(), lineNumber));
				setMarker(marker);
			}
		};
		run(getMarkerRule(resource), markerAttribs);
	}
	
	@Override
	public String getModelIdentifier() {
		return ClonkDebugModelPresentation.ID;
	}

}
