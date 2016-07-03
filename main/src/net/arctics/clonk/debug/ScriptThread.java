package net.arctics.clonk.debug;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;

import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.MutableRegion;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.debug.Target.Commands;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.util.StreamUtil;

public class ScriptThread extends DebugElement implements IThread {

	private static final StackFrame[] NO_STACKFRAMES = new StackFrame[0];

	private StackFrame[] stackFrames;

	private final Map<Script, Function[]> lineToFunctionMaps = new HashMap<Script, Function[]>();

	private void nullOut() {
		stackFrames = NO_STACKFRAMES;
	}

	public Script findScript(final String path, final Index index, final Set<Index> alreadySearched) throws CoreException {
		if (alreadySearched.contains(index)) {
			return null;
		}
		final Script script = index.findScriptByPath(path);
		if (script != null) {
			return script;
		}
		alreadySearched.add(index);
		if (index instanceof ProjectIndex) {
			for (final IProject proj : ((ProjectIndex) index).nature().getProject().getReferencedProjects()) {
				final ProjectIndex projIndex = ProjectIndex.get(proj);
				if (projIndex != null) {
					final Script _result = findScript(path, projIndex, alreadySearched);
					if (_result != null) {
						return _result;
					}
				}
			}
		}
		return null;
	}

	public void setStackTrace(final List<String> stackTrace) throws CoreException {
		final ProjectIndex index = ProjectIndex.get(getTarget().scenario().getProject());
		if (index == null) {
			nullOut();
			return;
		}
		final StackFrame[] newStackFrames = new StackFrame[stackTrace.size()];
		int stillToBeReused = stackFrames != null ? stackFrames.length : 0;
		for (int i = 0; i < stackTrace.size(); i++) {
			String sourcePath = stackTrace.get(i);

			if (sourcePath == null) {
				nullOut();
				return;
			}
			final String fullSourcePath = sourcePath;
			final int delim = sourcePath.lastIndexOf(':');
			final String linePart = sourcePath.substring(delim+1);
			final int line = Integer.parseInt(linePart);
			sourcePath = sourcePath.substring(0, delim);
			final Script script = findScript(sourcePath, index, new HashSet<Index>());
			final Function f = script != null ? funcAtLine(script, line) : null;
			final Object funObj = f != null ? f : fullSourcePath;
			if (stillToBeReused > 0 && stackFrames[stillToBeReused-1].getFunction().equals(funObj)) {
				newStackFrames[i] = stackFrames[--stillToBeReused];
				newStackFrames[i].setLine(line);
			} else {
				newStackFrames[i] = new StackFrame(this, f != null ? f : fullSourcePath, line);
			}
		}
		stackFrames = newStackFrames;
	}

	/**
	 * Return an array that acts as a map mapping line number to function at that line. Used for fast function lookups when only the line number is known.
	 * @return The pseudo-map for getting the function at some line.
	 */
	static Function[] calculateLineToFunctionMap(final Script script) {
		script.requireLoaded();
		String scriptText;
		try {
			scriptText = StreamUtil.stringFromInputStream(script.source().getContents());
		} catch (final CoreException e) {
			e.printStackTrace();
			return null;
		}
		int lineStart = 0;
		int lineEnd = 0;
		final List<Function> mappingAsList = new LinkedList<Function>();
		final MutableRegion region = new MutableRegion(0, 0);
		for (final BufferedScanner scanner = new BufferedScanner(scriptText); !scanner.reachedEOF();) {
			final int read = scanner.read();
			boolean newLine = false;
			switch (read) {
			case '\r':
				newLine = true;
				if (scanner.read() != '\n') {
					scanner.unread();
				}
				break;
			case '\n':
				newLine = true;
				break;
			default:
				lineEnd = scanner.tell();
			}
			if (newLine) {
				region.setOffset(lineStart);
				region.setLength(lineEnd-lineStart);
				Function f = script.funcAt(region);
				if (f == null) {
					f = script.funcAt(lineEnd);
				}
				mappingAsList.add(f);
				lineStart = scanner.tell();
				lineEnd = lineStart;
			}
		}
		return mappingAsList.toArray(new Function[mappingAsList.size()]);
	}

	private Function funcAtLine(final Script script, int line) {
		line--;
		Function[] map = lineToFunctionMaps.get(script);
		if (map == null) {
			map = calculateLineToFunctionMap(script);
			lineToFunctionMaps.put(script, map);
		}
		for (int x = line; line-x < 3; x--) {
			final Function f = x >= 0 && x < map.length ? map[x] : null;
			if (f != null) {
				return f;
			}
		}
		return null;
	}

	public ScriptThread(final Target target) {
		super(target);
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
	}

	@Override
	public IBreakpoint[] getBreakpoints() {
		return new IBreakpoint[0];
	}

	@Override
	public String getName() {
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
	public Object getAdapter(final Class adapter) {
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
