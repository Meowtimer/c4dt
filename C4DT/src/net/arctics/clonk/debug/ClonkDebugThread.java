package net.arctics.clonk.debug;

import net.arctics.clonk.debug.ClonkDebugTarget.Commands;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

public class ClonkDebugThread implements IThread {

	private ClonkDebugTarget target;
	
	public ClonkDebugThread(ClonkDebugTarget target) {
		this.target = target;
	}
	
	public ClonkDebugTarget getTarget() {
		return target;
	}

	@Override
	public IBreakpoint[] getBreakpoints() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() throws DebugException {
		return "Main Thread";
	}

	@Override
	public int getPriority() throws DebugException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IStackFrame[] getStackFrames() throws DebugException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IStackFrame getTopStackFrame() throws DebugException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasStackFrames() throws DebugException {
		// TODO Auto-generated method stub
		return false;
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

	@Override
	public boolean canResume() {
		return true;
	}

	@Override
	public boolean canSuspend() {
		return true;
	}

	@Override
	public boolean isSuspended() {
		return target.isSuspended();
	}

	@Override
	public void resume() throws DebugException {
		target.resume();
	}

	@Override
	public void suspend() throws DebugException {
		target.suspend();
	}

	@Override
	public boolean canStepInto() {
		return true;
	}

	@Override
	public boolean canStepOver() {
		return true;
	}

	@Override
	public boolean canStepReturn() {
		return true;
	}

	@Override
	public boolean isStepping() {
		return target.isSuspended();
	}

	@Override
	public void stepInto() throws DebugException {
		target.send(Commands.SUSPEND);
	}

	@Override
	public void stepOver() throws DebugException {
		// TODO Auto-generated method stub

	}

	@Override
	public void stepReturn() throws DebugException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean canTerminate() {
		return target.canTerminate();
	}

	@Override
	public boolean isTerminated() {
		return target.isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		target.terminate();
	}

}
