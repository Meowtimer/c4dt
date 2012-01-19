package net.arctics.clonk.debug;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Variable;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

public class ClonkDebugStackFrame extends ClonkDebugElement implements IStackFrame {

	private static final String NAME_FORMAT = Messages.ClonkDebugStackFrame_StackFrameMessage;
	
	private int line;
	private Object function;
	private ClonkDebugThread thread;
	private ClonkDebugVariable[] variables;
	
	public int index() throws DebugException {
		IStackFrame[] frames = thread.getStackFrames();
		for (int i = 0; i < frames.length; i++) {
			if (frames[i] == this)
				return i;
		}
		return -1; 
	}
	
	public ClonkDebugStackFrame(ClonkDebugThread thread, Object function, int line) {
		super(thread.getTarget());
		this.thread = thread;
		this.function = function;
		this.line = line;
		setVariables();
	}

	private void setVariables() {
		if (function instanceof Function) {
			Function f = (Function) function;
			List<ClonkDebugVariable> l = new LinkedList<ClonkDebugVariable>();
			for (Variable parm : f.parameters()) {
				if (parm.isActualParm())
					l.add(new ClonkDebugVariable(this, parm));
			}
			for (Variable local : f.getLocalVars()) {
				l.add(new ClonkDebugVariable(this, local));
			}
			variables = l.toArray(new ClonkDebugVariable[l.size()]);
		}
		else {
			variables = NO_VARIABLES;
		}
	}

	public void setLine(int line) {
		this.line = line;
	}

	public Object getFunction() {
		return function;
	}

	public void setFunction(Object function) {
		this.function = function;
	}

	@Override
	public int getCharEnd() throws DebugException {
		return -1;
	}

	@Override
	public int getCharStart() throws DebugException {
		return -1;
	}

	@Override
	public int getLineNumber() throws DebugException {
		return line;
	}

	@Override
	public String getName() throws DebugException {
		if (function instanceof Function)
			return String.format(NAME_FORMAT, ((Function)function).getScript().name(), ((Function) function).getLongParameterString(true), line);
		else if (function != null)
			return function.toString();
		else
			return null;
	}

	@Override
	public IRegisterGroup[] getRegisterGroups() throws DebugException {
		return null;
	}

	@Override
	public IThread getThread() {
		return thread;
	}

	@Override
	public ClonkDebugVariable[] getVariables() throws DebugException {
		return variables;
	}

	@Override
	public boolean hasRegisterGroups() throws DebugException {
		return false;
	}

	@Override
	public boolean hasVariables() throws DebugException {
		return variables.length > 0;
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
		return true;
	}

	@Override
	public void stepInto() throws DebugException {
		thread.stepInto();
	}

	@Override
	public void stepOver() throws DebugException {
		thread.stepOver();
	}

	@Override
	public void stepReturn() throws DebugException {
		thread.stepReturn();
	}

	@Override
	public boolean canResume() {
		return true;
	}

	@Override
	public boolean canSuspend() {
		return !isSuspended();
	}

	@Override
	public boolean isSuspended() {
		return thread.isSuspended();
	}

	@Override
	public void resume() throws DebugException {
		thread.resume();
	}

	@Override
	public void suspend() throws DebugException {
		thread.suspend();
	}

	@Override
	public boolean canTerminate() {
		return true;
	}

	@Override
	public boolean isTerminated() {
		return thread.isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
		thread.terminate();
	}
	
	public String getSourcePath() {
		if (function instanceof Function) {
			Function f = (Function) function;
			IResource r = f.getScript().resource();
			if (r instanceof IContainer)
				return r.getProjectRelativePath().append("Script.c").toOSString(); //$NON-NLS-1$
			else if (r != null)
				return r.getProjectRelativePath().toOSString();
		}
		return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClonkDebugStackFrame) {
			ClonkDebugStackFrame other = (ClonkDebugStackFrame) obj;
			return other.function.equals(function) && other.line == line;
		}
		return false;
	}

}
