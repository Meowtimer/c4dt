package net.arctics.clonk.debug;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IVariable;

public class ClonkDebugElement implements IDebugElement {

	protected static final IVariable[] NO_VARIABLES = new IVariable[0];
	
	private ClonkDebugTarget target;

	public ClonkDebugElement(ClonkDebugTarget target) {
		this.target = target;
	}

	public ClonkDebugTarget getTarget() {
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
	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected void fireEvent(DebugEvent event) {
		DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {event});
	}
	
	public void fireResumeEvent(int detail) {
		fireEvent(new DebugEvent(this, DebugEvent.RESUME, detail));
	}
	
	public void fireSuspendEvent(int detail) {
		fireEvent(new DebugEvent(this, DebugEvent.SUSPEND, detail));
	}
	
	protected void fireTerminateEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
	}
	
}
