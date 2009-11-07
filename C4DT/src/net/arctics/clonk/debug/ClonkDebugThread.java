package net.arctics.clonk.debug;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.debug.ClonkDebugTarget.Commands;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

public class ClonkDebugThread extends ClonkDebugElement implements IThread {
	
	private static final IStackFrame[] NO_STACKFRAMES = new IStackFrame[0];
	
	private String sourcePath;
	private IStackFrame[] stackFrames;
	private C4ScriptBase script;
	
	private void nullOut() {
		script = null;
		stackFrames = NO_STACKFRAMES;
	}
	
	public void setSourcePath(String sourcePath) throws CoreException {
		if (sourcePath == null) {
			nullOut();
			return;
		}
		String fullSourcePath = sourcePath;
		int delim = sourcePath.lastIndexOf(':');
		String linePart = sourcePath.substring(delim+1);
		int line = Integer.parseInt(linePart);
		sourcePath = sourcePath.substring(0, delim);
		if (this.sourcePath == null || this.sourcePath.equals(sourcePath)) {
			this.sourcePath = sourcePath;
			ClonkProjectNature nature = ClonkProjectNature.getClonkNature(getTarget().getScenario().getProject());
			if (nature != null) {
				IResource resInProject = nature.getProject().findMember(new Path(sourcePath));
				if (resInProject != null && (script = Utilities.getScriptForResource(resInProject)) != null) {
					// yuppei, local script found
				}
				else if ((script = ClonkCore.getDefault().getExternIndex().findScriptByPath(sourcePath)) != null) {
					// an external is just fine too
				}
			}
		}
		if (script != null) {
			C4Function f = script.funcAtLine(line);
			stackFrames = new IStackFrame[] {
				new ClonkDebugStackFrame(this, f != null ? f : fullSourcePath, line)
			};
		}
	}
	
	public ClonkDebugThread(ClonkDebugTarget target) {
		super(target);
	}

	@Override
	public IBreakpoint[] getBreakpoints() {
		return new IBreakpoint[0];
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
		return stackFrames;
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
		return getTarget().isSuspended();
	}

	@Override
	public void resume() throws DebugException {
		getTarget().resume();
	}

	@Override
	public void suspend() throws DebugException {
		getTarget().suspend();
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
		return getTarget().isSuspended();
	}

	@Override
	public void stepInto() throws DebugException {
		getTarget().send(Commands.SUSPEND);
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
		return getTarget().canTerminate();
	}

	@Override
	public boolean isTerminated() {
		return getTarget().isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		getTarget().terminate();
	}

}
