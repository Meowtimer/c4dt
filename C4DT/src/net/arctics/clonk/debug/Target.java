package net.arctics.clonk.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.FileExtension;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.ui.debug.ClonkDebugModelPresentation;
import net.arctics.clonk.util.ICreate;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

/**
 * Debug target representing a running Clonk engine.
 * @author madeen
 *
 */
public class Target extends DebugElement implements IDebugTarget {

	/**
	 * Commands to send through the debug pipe.
	 * @author madeen
	 *
	 */
	public static class Commands {
		public static final String RESUME = "GO"; //$NON-NLS-1$
		public static final String SUSPEND = "STP"; //$NON-NLS-1$
		public static final String STEPOVER = "STO"; //$NON-NLS-1$
		public static final String STEP = "STP"; //$NON-NLS-1$
		public static final String STEPRETURN = "STR"; //$NON-NLS-1$
		public static final String QUITSESSION = "BYE"; //$NON-NLS-1$
		public static final String STACKTRACE = "STA"; //$NON-NLS-1$
		public static final String TOGGLEBREAKPOINT = "TBR"; //$NON-NLS-1$
		public static final String VAR = "VAR"; //$NON-NLS-1$
		public static final String SENDSTACKTRACE = "SST"; //$NON-NLS-1$
		public static final String ENDSTACKTRACE = "EST"; //$NON-NLS-1$
		public static final String AT = "AT"; //$NON-NLS-1$
		public static final String EXEC = "EXC"; //$NON-NLS-1$
		public static final String EVALUATIONRESULT = "EVR"; //$NON-NLS-1$
		public static final String GO = "GO"; //$NON-NLS-1$
		public static final String POSITION = "POS"; //$NON-NLS-1$
	}

	/**
	 * Result type for ILineReceivedListener.lineReceived.
	 * Used to decide whether a listener processed a line and whether it should be removed from the queue or not.
	 * @author madeen
	 */
	public enum LineReceivedResult {
		/** Has been processed but should not be removed. */
		ProcessedDontRemove,
		/** Has been processed and should be removed. */
		ProcessedRemove,
		/** Has not been processed and should not be removed. */
		NotProcessedDontRemove,
		/** Not processed but to be removed anyway. */
		NotProcessedRemove
	}

	/**
	 * Interface to be implemented by objects interested in lines sent by the engine.
	 * @author madeen
	 *
	 */
	public interface ILineReceivedListener {
		/**
		 * Notification invoked when the engine did send a line.
		 * @param line The line as string
		 * @param target The associated target
		 * @return Hint for the target whether the line should further be processed or not.
		 * @throws IOException
		 */
		public LineReceivedResult lineReceived(String line, Target target) throws IOException;
		/**
		 * Returns true if this listener gets exclusive notifications about lines received.
		 * @return
		 */
		public boolean exclusive();
		/**
		 * Whether this listener is currently active.
		 * @return
		 */
		public boolean active();
	}

	public static final int CONNECTION_ATTEMPT_WAITTIME = 2000;

	private final ILaunch launch;
	private final IProcess process;
	private final ScriptThread thread;
	private final IThread[] threads;
	private Socket socket;
	private PrintWriter socketWriter;
	private BufferedReader socketReader;
	private boolean suspended;
	private final IResource scenario;

	private final List<ILineReceivedListener> lineReceiveListeners = new LinkedList<ILineReceivedListener>();

	/**
	 * Request a line received listener of a certain type. A new one won't be created if one already exists
	 * exactly matching the class specified by create.cls().
	 * @param <T> The type of listener to request
	 * @param create Object creation proxy responsible for specifying the class and if necessary creating a new listener.
	 * @return A listener of the requested type.
	 */
	@SuppressWarnings("unchecked")
	public <T extends ILineReceivedListener> T requestLineReceivedListener(final ICreate<T> create) {
		final Class<? extends ILineReceivedListener> cls = create.cls();
		return lineReceiveListeners.stream()
			.filter(listener -> listener.getClass() == cls)
			.map(l -> (T)l)
			.findFirst()
			.orElseGet(() -> addLineReceiveListener(create.create()));
	}

	/**
	 * Add a line received listener to the internal list.
	 * @param listener The listener to add
	 */
	public <T extends ILineReceivedListener> T addLineReceiveListener(final T listener) {
		System.out.println("Adding " + listener.toString()); //$NON-NLS-1$
		synchronized (lineReceiveListeners) {
			lineReceiveListeners.add(0, listener);
		}
		return listener;
	}

	/**
	 * Remove a line received listener from the internal list.
	 * @param listener The listener to remove
	 */
	public void removeLineReceiveListener(final ILineReceivedListener listener) {
		System.out.println("Removing " + listener.toString()); //$NON-NLS-1$
		synchronized (lineReceiveListeners) {
			lineReceiveListeners.remove(listener);
		}
	}

	private class EventDispatchJob extends Job implements ILineReceivedListener {

		public EventDispatchJob(final String name) { super(name); }

		private final List<String> stackTrace = new ArrayList<String>(10);

		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			addLineReceiveListener(this);
			String event = ""; //$NON-NLS-1$
			while (!isTerminated() && event != null)
				try {
					event = receive();
					if (event != null && event.length() > 0) {
						ILineReceivedListener listenerToRemove = null;
						boolean processed = false;
						synchronized (lineReceiveListeners) {
							Outer: for (final ILineReceivedListener listener : lineReceiveListeners) {
								if (!listener.active())
									continue;
								switch (listener.lineReceived(event, Target.this)) {
								case NotProcessedDontRemove:
									if (listener.exclusive())
										break Outer;
									break;
								case ProcessedDontRemove:
									processed = true;
									break Outer;
								case ProcessedRemove:
									listenerToRemove = listener;
									processed = true;
									break Outer;
								case NotProcessedRemove:
									listenerToRemove = listener;
									break Outer;
								}
							}
						}
						if (!processed)
							lostLines.offer(event);
						if (listenerToRemove != null) {
							removeLineReceiveListener(listenerToRemove);
							reshuffleLines();
						}
					}
				} catch (final IOException e) {
					break;
				}
			terminated();
			return Status.OK_STATUS;
		}

		@Override
		public boolean exclusive() { return false; }
		@Override
		public boolean active() { return true; }

		@Override
		public LineReceivedResult lineReceived(String event, final Target target) throws IOException {
			if (event.startsWith(Commands.POSITION)) {
				final String sourcePath = event.substring(Commands.POSITION.length()+1, event.length());
				stackTrace.clear();
				stackTrace.add(sourcePath);

				send(Commands.SENDSTACKTRACE);
				while (!isTerminated() && event != null) {
					event = receive();
					if (event != null && event.length() > 0)
						if (event.equals(Commands.ENDSTACKTRACE))
							break;
						else if (event.startsWith(Commands.AT + " ")) { //$NON-NLS-1$
							if (stackTrace.size() > 512) {
								System.out.println("Runaway stacktrace"); //$NON-NLS-1$
								break;
							} else
								stackTrace.add(event.substring(Commands.AT.length()+1));
						}
						else
							break;
				}

				stoppedWithStackTrace(stackTrace);

				try {
					if (thread.getTopStackFrame() != null && thread.getTopStackFrame().getVariables() != null) {
						final DebugVariable[] varArray = ((StackFrame)thread.getTopStackFrame()).getVariables();
						final Map<String, DebugVariable> vars = new HashMap<String, DebugVariable>(varArray.length);
						for (final DebugVariable var : varArray)
							vars.put(var.getName(), var);
						for (final DebugVariable var : vars.values())
							send(String.format("%s %s", Commands.VAR, var.getName())); //$NON-NLS-1$
						while (!isTerminated() && event != null && !vars.isEmpty()) {
						//	System.out.println("missing " + vars.toString());
							event = receive();
							if (event != null && event.length() > 0)
								if (event.startsWith(Commands.VAR)) {
									event = event.substring(Commands.VAR.length());
									final BufferedScanner scanner = new BufferedScanner(event);
									scanner.read();
									final String varName = scanner.readIdent();
									scanner.read();
									final String varType = scanner.readIdent();
									scanner.read();
									final String varValue = event.substring(scanner.tell());
									if (varName != null && varType != null && varValue != null) {
										final DebugVariable var = vars.get(varName);
										if (var != null) {
											var.getValue().setValue(varValue, PrimitiveType.fromString(varType));
											vars.remove(varName);
										}
									}
								}
						}
					}
				} catch (final DebugException e) {
					e.printStackTrace();
				}

				if (!suspended) {
					suspended = true;
					thread.fireSuspendEvent(DebugEvent.CLIENT_REQUEST);
				}
				return LineReceivedResult.ProcessedDontRemove;
			}
			return LineReceivedResult.NotProcessedDontRemove;
		}

	}

	synchronized void setConnectionObjects(final Socket socket, final PrintWriter socketWriter, final BufferedReader socketReader) {
		this.socket = socket;
		this.socketWriter = socketWriter;
		this.socketReader = socketReader;

		fireEvent(new DebugEvent(this, DebugEvent.CREATE));

		send(""); //$NON-NLS-1$
		send(Commands.STEP); // suspend in order to set breakpoints
		setBreakpoints();
		send(Commands.GO); // go!

		new EventDispatchJob("Clonk Debugger Event Dispatch").schedule(); //$NON-NLS-1$
	}

	private void setBreakpoints() {
		final IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(ClonkDebugModelPresentation.ID);
		for (final IBreakpoint b : breakpoints)
			try {
				if (b.isEnabled())
					breakpointAdded(b);
			} catch (final CoreException e) {
				e.printStackTrace();
			}
	}

	private void stoppedWithStackTrace(final List<String> stackTrace) {
		try {
			thread.setStackTrace(stackTrace);
		} catch (final CoreException e) {
			e.printStackTrace();
			return;
		}
		thread.fireSuspendEvent(DebugEvent.STEP_INTO);
	}

	public Target(final ILaunch launch, final IProcess process, final Integer port, final IResource scenario) throws Exception {
		super(null);
		this.launch = launch;
		//this.launch.setSourceLocator(new SourceLookupDirector());
		this.process = process;
		this.thread = new ScriptThread(this);
		this.threads = new IThread[] {thread};
		this.scenario = scenario;
		if (port != null) {
			new ConnectionJob(this, "Clonk Debugger Connection Job", port).schedule(); //$NON-NLS-1$
			DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
		}
	}

	public IResource scenario() { return scenario; }
	@Override
	public String getName() throws DebugException { return "Clonk DebugTarget"; } //$NON-NLS-1$
	@Override
	public IProcess getProcess() { return process; }
	@Override
	public IThread[] getThreads() throws DebugException { return threads; }
	@Override
	public boolean hasThreads() throws DebugException { return threads.length > 0; }
	@Override
	public boolean supportsBreakpoint(final IBreakpoint breakpoint) { return breakpoint instanceof Breakpoint; }
	@Override
	public IDebugTarget getDebugTarget() { return this; }
	@Override
	public ILaunch getLaunch() { return launch; }
	@Override
	public String getModelIdentifier() { return ClonkDebugModelPresentation.ID; }
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(final Class adapter) { return null; }
	@Override
	public boolean canTerminate() { return !isTerminated(); }
	@Override
	public boolean isTerminated() { return process.isTerminated(); }
	@Override
	public boolean canResume() { return !isTerminated() && isSuspended(); }
	@Override
	public boolean canSuspend() { return !isTerminated() && !isSuspended(); }
	@Override
	public boolean isSuspended() { return suspended; }
	@Override
	public boolean canDisconnect() { return !isDisconnected(); }
	@Override
	public boolean isDisconnected() { return socket == null; }
	@Override
	public IMemoryBlock getMemoryBlock(final long startAddress, final long length) throws DebugException { return null; }
	@Override
	public boolean supportsStorageRetrieval() { return false; }

	/*+
	 * Called when the engine terminated for whatever reason.
	 */
	public void terminated() {
		try {
			terminate();
		} catch (final DebugException e) {}
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
		fireTerminateEvent();
	}

	@Override
	public void terminate() throws DebugException {
		disconnect();
		process.terminate();
	}

	public void send(final String command, final ILineReceivedListener listener) {
		if (listener != null)
			addLineReceiveListener(listener);
		synchronized (socketWriter) {
			System.out.println("Sending " + command + " to engine"); //$NON-NLS-1$ //$NON-NLS-2$
			socketWriter.println(command);
			socketWriter.flush();
		}
	}

	public final void send(final String command) {
		send(command, null);
	}

	private Queue<String> lostLines = new LinkedList<String>();
	private Queue<String> reshuffledLines;

	public void reshuffleLines() {
		reshuffledLines = lostLines;
		lostLines = new LinkedList<String>();
	}

	public final String receive() throws IOException {
		if (reshuffledLines != null) {
			final String lost = reshuffledLines.poll();
			if (lost != null)
				return lost;
		}
		String r = socketReader.readLine();
		if (r != null) {
			if (r.charAt(r.length()-1) == 0)
				r = r.substring(0, r.length()-1);
			System.out.println("Got line from Clonk: " + r); //$NON-NLS-1$
		}
		return r;
	}

	@Override
	public void resume() throws DebugException {
		send(Commands.RESUME);
		suspended = false;
		fireResumeEvent(DebugEvent.CLIENT_REQUEST);
	}

	@Override
	public void suspend() throws DebugException {
		send(Commands.SUSPEND);
	}

	@Override
	public void breakpointAdded(final IBreakpoint breakpoint) {
		try {
			if (breakpoint instanceof Breakpoint) {
				final Breakpoint bp = (Breakpoint) breakpoint;
				final IResource res = bp.getMarker().getResource();
				final IPath relPath = relativePath(res);
				send(String.format("%s %s:%d", Commands.TOGGLEBREAKPOINT, relPath.toOSString(), bp.getLineNumber())); //$NON-NLS-1$
			}
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}

	private IPath relativePath(final IResource res) {
		final IPath relPath = res.getProjectRelativePath();
		final ClonkProjectNature cpn = ClonkProjectNature.get(res);
		final String scenSuffix = "." + cpn.index().engine().settings().canonicalToConcreteExtension().get(FileExtension.ScenarioGroup);
		for (int i = relPath.segmentCount()-1; i >= 0; i--)
			if (relPath.segment(i).endsWith(scenSuffix))
				return relPath.removeFirstSegments(i);
		return relPath;
	}

	@Override
	public void breakpointChanged(final IBreakpoint breakpoint, final IMarkerDelta delta) {
		if (delta.getAttribute(IBreakpoint.ENABLED) != null)
			breakpointAdded(breakpoint);
	}

	@Override
	public void breakpointRemoved(final IBreakpoint breakpoint, final IMarkerDelta delta) {
		try {
			if (breakpoint.isEnabled())
				breakpointAdded(breakpoint);
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void disconnect() throws DebugException {
		if (socketWriter != null) {
			socketWriter.close();
			socketWriter = null;
		}
		if (socketReader != null) {
			try {
				socketReader.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			socketReader = null;
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			socket = null;
		}
		lineReceiveListeners.clear();
	}

}
