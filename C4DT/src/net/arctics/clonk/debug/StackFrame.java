package net.arctics.clonk.debug;

import static net.arctics.clonk.util.Utilities.as;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Function.PrintParametersOptions;
import net.arctics.clonk.c4script.Variable;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

public class StackFrame extends DebugElement implements IStackFrame {

	private static final String NAME_FORMAT = Messages.ClonkDebugStackFrame_StackFrameMessage;

	private int line;
	private Object function;
	private final ScriptThread thread;
	private DebugVariable[] variables;

	public int index() throws DebugException {
		final IStackFrame[] frames = thread.getStackFrames();
		for (int i = 0; i < frames.length; i++)
			if (frames[i] == this)
				return i;
		return -1;
	}

	public StackFrame(ScriptThread thread, Object function, int line) {
		super(thread.getTarget());
		this.thread = thread;
		this.function = function;
		this.line = line;
		setVariables();
	}

	private void setVariables() {
		if (function instanceof Function) {
			final Function f = (Function) function;
			final List<DebugVariable> l = new LinkedList<DebugVariable>();
			for (final Variable parm : f.parameters())
				if (parm.isActualParm())
					l.add(new DebugVariable(this, parm));
			for (final Variable local : f.locals())
				l.add(new DebugVariable(this, local));
			variables = l.toArray(new DebugVariable[l.size()]);
		} else
			variables = NO_VARIABLES;
	}

	@Override
	public String getName() throws DebugException {
		final Function f = as(function, Function.class);
		if (f != null)
			return String.format(NAME_FORMAT, f.script().name(), f.parameterString
				(new PrintParametersOptions(f.script().typings().get(f), true, true, false)), line);
		else if (function != null)
			return function.toString();
		else
			return null;
	}

	public void setLine(int line) { this.line = line; }
	public Object getFunction() { return function; }
	public void setFunction(Object function) { this.function = function; }
	@Override
	public int getCharEnd() throws DebugException { return -1; }
	@Override
	public int getCharStart() throws DebugException { return -1; }
	@Override
	public int getLineNumber() throws DebugException { return line; }
	@Override
	public IRegisterGroup[] getRegisterGroups() throws DebugException { return null; }
	@Override
	public IThread getThread() { return thread; }
	@Override
	public DebugVariable[] getVariables() throws DebugException { return variables; }
	@Override
	public boolean hasRegisterGroups() throws DebugException { return false; }
	@Override
	public boolean hasVariables() throws DebugException { return variables.length > 0; }
	@Override
	public boolean canStepInto() { return true; }
	@Override
	public boolean canStepOver() { return true; }
	@Override
	public boolean canStepReturn() { return true; }
	@Override
	public boolean isStepping() { return true; }
	@Override
	public void stepInto() throws DebugException { thread.stepInto(); }
	@Override
	public void stepOver() throws DebugException { thread.stepOver(); }
	@Override
	public void stepReturn() throws DebugException { thread.stepReturn(); }
	@Override
	public boolean canResume() { return true; }
	@Override
	public boolean canSuspend() { return !isSuspended(); }
	@Override
	public boolean isSuspended() { return thread.isSuspended(); }
	@Override
	public void resume() throws DebugException { thread.resume(); }
	@Override
	public void suspend() throws DebugException { thread.suspend(); }
	@Override
	public boolean canTerminate() { return true; }
	@Override
	public boolean isTerminated() { return thread.isTerminated(); }
	@Override
	public void terminate() throws DebugException { thread.terminate(); }

	public String getSourcePath() {
		if (function instanceof Function) {
			final Function f = (Function) function;
			final IResource r = f.script().resource();
			if (r instanceof IContainer)
				return r.getProjectRelativePath().append("Script.c").toOSString(); //$NON-NLS-1$
			else if (r != null)
				return r.getProjectRelativePath().toOSString();
		}
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StackFrame) {
			final StackFrame other = (StackFrame) obj;
			return other.function.equals(function) && other.line == line;
		}
		return false;
	}

}
