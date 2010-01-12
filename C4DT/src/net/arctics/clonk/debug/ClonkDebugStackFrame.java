package net.arctics.clonk.debug;

import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4Variable;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;

public class ClonkDebugStackFrame extends ClonkDebugElement implements IStackFrame {

	private static final String NAME_FORMAT = "%s::%s line: %d";
	
	private int line;
	private Object function;
	private ClonkDebugThread thread;
	private IVariable[] variables;
	
	public ClonkDebugStackFrame(ClonkDebugThread thread, Object function, int line) {
		super(thread.getTarget());
		this.thread = thread;
		this.function = function;
		this.line = line;
		setVariables();
	}

	private void setVariables() {
		if (function instanceof C4Function) {
			C4Function f = (C4Function) function;
			variables = new IVariable[f.getParameters().size()+f.getLocalVars().size()];
			int i = 0;
			for (C4Variable parm : f.getParameters()) {
				variables[i++] = new ClonkDebugVariable(this, parm);
			}
			for (C4Variable local : f.getLocalVars()) {
				variables[i++] = new ClonkDebugVariable(this, local);
			}
		}
		else {
			variables = NO_VARIABLES;
		}
	}

	public int getLine() {
		return line;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public C4Function getFunction() {
		return (C4Function) (function instanceof C4Function ? function : null);
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
		if (function instanceof C4Function)
			return String.format(NAME_FORMAT, ((C4Function)function).getScript().toString(), ((C4Function) function).getLongParameterString(true), line);
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
	public IVariable[] getVariables() throws DebugException {
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
		if (function instanceof C4Function) {
			C4Function f = (C4Function) function;
			IResource r = f.getScript().getResource();
			if (r instanceof IContainer)
				return r.getProjectRelativePath().append("Script.c").toOSString();
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
