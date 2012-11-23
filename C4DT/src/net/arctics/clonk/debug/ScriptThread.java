package net.arctics.clonk.debug;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.arctics.clonk.debug.Target.Commands;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

public class ScriptThread extends DebugElement implements IThread {
	
	private static final StackFrame[] NO_STACKFRAMES = new StackFrame[0];
	
	private StackFrame[] stackFrames;
	
	private Map<Script, Function[]> lineToFunctionMaps = new HashMap<Script, Function[]>(); 
	
	private void nullOut() {
		stackFrames = NO_STACKFRAMES;
	}
	
	public Script findScript(String path, Index index, Set<Index> alreadySearched) throws CoreException {
		if (alreadySearched.contains(index))
			return null;
		Script script = index.findScriptByPath(path);
		if (script != null)
			return script;
		alreadySearched.add(index);
		if (index instanceof ProjectIndex) {
			for (IProject proj : ((ProjectIndex) index).project().getReferencedProjects()) {
				ProjectIndex projIndex = ProjectIndex.get(proj);
				if (projIndex != null) {
					Script _result = findScript(path, projIndex, alreadySearched);
					if (_result != null)
						return _result;
				}
			}
		}
		return null;
	}

	public void setStackTrace(List<String> stackTrace) throws CoreException {
		ProjectIndex index = ProjectIndex.get(getTarget().getScenario().getProject());
		if (index == null) {
			nullOut();
			return;
		}
		StackFrame[] newStackFrames = new StackFrame[stackTrace.size()];
		int stillToBeReused = stackFrames != null ? stackFrames.length : 0;
		for (int i = 0; i < stackTrace.size(); i++) {
			String sourcePath = stackTrace.get(i);
			
			if (sourcePath == null) {
				nullOut();
				return;
			}
			String fullSourcePath = sourcePath;
			int delim = sourcePath.lastIndexOf(':');
			String linePart = sourcePath.substring(delim+1);
			int line = Integer.parseInt(linePart)+1;
			sourcePath = sourcePath.substring(0, delim);
			Script script = findScript(sourcePath, index, new HashSet<Index>());
			Function f = script != null ? funcAtLine(script, line) : null;
			Object funObj = f != null ? f : fullSourcePath;
			if (stillToBeReused > 0) {
				if (stackFrames[stillToBeReused-1].getFunction().equals(funObj)) {
					newStackFrames[i] = stackFrames[--stillToBeReused];
					newStackFrames[i].setLine(line);
					continue;
				}
			}
			newStackFrames[i] = new StackFrame(this, f != null ? f : fullSourcePath, line);
		}
		stackFrames = newStackFrames;
	}
	
	private Function funcAtLine(Script script, int line) {
		line--;
		Function[] map = lineToFunctionMaps.get(script);
		if (map == null) {
			map = script.calculateLineToFunctionMap();
			lineToFunctionMaps.put(script, map);
		}
		return line >= 0 && line < map.length ? map[line] : null;
	}

	public ScriptThread(Target target) {
		super(target);
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
	}

	@Override
	public IBreakpoint[] getBreakpoints() {
		return new IBreakpoint[0];
	}

	@Override
	public String getName() throws DebugException {
		return Messages.MainThread;
	}

	@Override
	public int getPriority() throws DebugException {
		return 1;
	}

	@Override
	public IStackFrame[] getStackFrames() throws DebugException {
		return hasStackFrames() ? stackFrames : NO_STACKFRAMES;
	}

	@Override
	public IStackFrame getTopStackFrame() throws DebugException {
		return hasStackFrames() ? stackFrames[0] : null;
	}

	@Override
	public boolean hasStackFrames() throws DebugException {
		return stackFrames != null && stackFrames.length > 0 && isSuspended();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean canResume() {
		return getTarget().canResume();
	}

	@Override
	public boolean canSuspend() {
		return getTarget().canSuspend();
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
		//fireSuspendEvent(DebugEvent.CLIENT_REQUEST);
	}

	@Override
	public boolean canStepInto() {
		return isSuspended();
	}

	@Override
	public boolean canStepOver() {
		return isSuspended();
	}

	@Override
	public boolean canStepReturn() {
		return isSuspended();
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
		getTarget().send(Commands.STEPOVER);
	}

	@Override
	public void stepReturn() throws DebugException {
		getTarget().send(Commands.STEPRETURN);
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
		fireTerminateEvent();
	}

}
