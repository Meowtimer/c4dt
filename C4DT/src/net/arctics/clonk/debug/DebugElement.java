package net.arctics.clonk.debug;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;

public class DebugElement implements IDebugElement {

	protected static final DebugVariable[] NO_VARIABLES = new DebugVariable[0];
	
	private final Target target;

	public DebugElement(final Target target) {
		this.target = target;
	}

	public Target getTarget() {
		return target;
	}
	
	@Override
	public IDebugTarget getDebugTarget() {
		return target;
	}

	@Override
	public ILaunch getLaunch() {
		return target.getLaunch();
	}

	@Override
	public String getModelIdentifier() {
		return target.getModelIdentifier();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(final Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected void fireEvent(final DebugEvent event) {
		DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {event});
	}
	
	public void fireResumeEvent(final int detail) {
		fireEvent(new DebugEvent(this, DebugEvent.RESUME, detail));
	}
	
	public void fireSuspendEvent(final int detail) {
		fireEvent(new DebugEvent(this, DebugEvent.SUSPEND, detail));
	}
	
	protected void fireTerminateEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
	}
	
}
