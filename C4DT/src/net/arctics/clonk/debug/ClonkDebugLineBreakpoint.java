package net.arctics.clonk.debug;

import net.arctics.clonk.ClonkCore;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;

public class ClonkDebugLineBreakpoint implements IBreakpoint {

	private IMarker marker;
	
	public ClonkDebugLineBreakpoint(IResource resource, int lineNumber) throws CoreException {
		IMarker marker = resource.createMarker(
				"org.eclipse.debug.examples.core.pda.lineBreakpoint.marker"); //$NON-NLS-1$
		setMarker(marker);
		setEnabled(true);
		marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		marker.setAttribute(IBreakpoint.ID, ClonkCore.PLUGIN_ID);
	}
	
	@Override
	public void delete() throws CoreException {
		marker.delete();
	}

	@Override
	public IMarker getMarker() {
		return marker;
	}

	@Override
	public String getModelIdentifier() {
		return ClonkCore.PLUGIN_ID;
	}

	@Override
	public boolean isEnabled() throws CoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isPersisted() throws CoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRegistered() throws CoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setEnabled(boolean enabled) throws CoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMarker(IMarker marker) throws CoreException {
		this.marker = marker;
	}

	@Override
	public void setPersisted(boolean registered) throws CoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRegistered(boolean registered) throws CoreException {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}

}
