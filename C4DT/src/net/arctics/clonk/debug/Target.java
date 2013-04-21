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

import net.arctics.clonk.c4script.PrimitiveType;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.ui.debug.ClonkDebugModelPresentation;
import net.arctics.clonk.util.ICreate;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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
	public <T extends ILineReceivedListener> T requestLineReceivedListener(ICreate<T> create) {
		Class<T> cls = create.cls();
		for (ILineReceivedListener listener : lineReceiveListeners)
			if (listener.getClass() == cls)
				return (T) listener;
		T result = create.create();
		addLineReceiveListener(result);
		return result;
	}

	/**
	 * Add a line received listener to the internal list.
	 * @param listener The listener to add
	 */
	public void addLineReceiveListener(ILineReceivedListener listener) {
		System.out.println("Adding " + listener.toString()); //$NON-NLS-1$
		synchronized (lineReceiveListeners) {
			lineReceiveListeners.add(0, listener);
		}
	}
	
	/**
	 * Remove a line received listener from the internal list.
	 * @param listener The listener to remove
	 */
	public void removeLineReceiveListener(ILineReceivedListener listener) {
		System.out.println("Removing " + listener.toString()); //$NON-NLS-1$
		synchronized (lineReceiveListeners) {
			lineReceiveListeners.remove(listener);
		}
	}
	
	/**
	 * Return the socket writer used to communicate with the engine.
	 * @return
	 */
	public PrintWriter getSocketWriter() {
		return socketWriter;
	}

	/**
	 * Return the socket reader used to receive commands from the engine.
	 * @return
	 */
	public BufferedReader getSocketReader() {
		return socketReader;
	}
	
	private class EventDispatchJob extends Job implements ILineReceivedListener {
		
		public EventDispatchJob(String name) {
			super(name);
		}
		
		private final List<String> stackTrace = new ArrayList<String>(10);
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			addLineReceiveListener(this);
			String event = ""; //$NON-NLS-1$
			while (!isTerminated() && event != null)
				try {
					event = receive();
					if (event != null && event.length() > 0) {
						ILineReceivedListener listenerToRemove = null;
						boolean processed = false;
						synchronized (lineReceiveListeners) {
							Outer: for (ILineReceivedListener listener : lineReceiveListeners) {
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
				} catch (IOException e) {
					break;
				}
			terminated();
			return Status.OK_STATUS;
		}

		@Override
		public boolean exclusive() {
			return false;
		}
		
		@Override
		public boolean active() {
			return true;
		}

		@Override
		public LineReceivedResult lineReceived(String event, Target target) throws IOException {
			if (event.startsWith(Commands.POSITION)) { 
				String sourcePath = event.substring(Commands.POSITION.length()+1, event.length());
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
						DebugVariable[] varArray = ((StackFrame)thread.getTopStackFrame()).getVariables();
						Map<String, DebugVariable> vars = new HashMap<String, DebugVariable>(varArray.length);
						for (DebugVariable var : varArray)
							vars.put(var.getName(), var);
						for (DebugVariable var : vars.values())
							send(String.format("%s %s", Commands.VAR, var.getName())); //$NON-NLS-1$
						while (!isTerminated() && event != null && !vars.isEmpty()) {
						//	System.out.println("missing " + vars.toString());
							event = receive();
							if (event != null && event.length() > 0)
								if (event.startsWith(Commands.VAR)) { 
									event = event.substring(Commands.VAR.length()); 
									BufferedScanner scanner = new BufferedScanner(event);
									scanner.read();
									String varName = scanner.readIdent();
									scanner.read();
									String varType = scanner.readIdent();
									scanner.read();
									String varValue = event.substring(scanner.tell());
									if (varName != null && varType != null && varValue != null) {
										DebugVariable var = vars.get(varName);
										if (var != null) {
											var.getValue().setValue(varValue, PrimitiveType.fromString(varType));
											vars.remove(varName);
										}
									}
								}
						}
					}
				} catch (DebugException e) {
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
	
	synchronized void setConnectionObjects(Socket socket, PrintWriter socketWriter, BufferedReader socketReader_) {
		this.socket = socket;
		this.socketWriter = socketWriter;
		this.socketReader = socketReader_;
		
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
		
		send(""); //$NON-NLS-1$
		send(Commands.STEP); // suspend in order to set breakpoints 
		setBreakpoints();
		send(Commands.GO); // go! 
		
		new EventDispatchJob("Clonk Debugger Event Dispatch").schedule(); //$NON-NLS-1$
	}
	
	private void setBreakpoints() {
		IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(ClonkDebugModelPresentation.ID);
		for (IBreakpoint b : breakpoints)
			try {
				if (b.isEnabled())
					breakpointAdded(b);
			} catch (CoreException e) {
				e.printStackTrace();
			}
	}
	
	private void stoppedWithStackTrace(List<String> stackTrace) {
		try {
			thread.setStackTrace(stackTrace);
		} catch (CoreException e) {
			e.printStackTrace();
			return;
		}
		thread.fireSuspendEvent(DebugEvent.STEP_INTO);
	}

	private static Map<IResource, Target> existingTargets = new HashMap<IResource, Target>();
	
	public static Target existingDebugTargetForScenario(IResource scenario) {
		return existingTargets.get(scenario);
	}
	
	public Target(ILaunch launch, IProcess process, int port, IResource scenario) throws Exception {
		super(null);

		synchronized (existingTargets) {
			assert(existingTargets.get(scenario) == null);
			existingTargets.put(scenario, this);
		}
		
		this.launch = launch;
		this.process = process;
		this.thread = new ScriptThread(this);
		this.threads = new IThread[] {thread};
		this.scenario = scenario;

		new ConnectionJob(this, "Clonk Debugger Connection Job", port).schedule(); //$NON-NLS-1$

		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);

		//launch.setsou

	}

	public IResource getScenario() {
		return scenario;
	}

	/*
	private void abort(String message, Exception e) throws Exception {
		System.out.println(message);
		throw e;
	}*/

	@Override
	public String getName() throws DebugException {
		return "Clonk DebugTarget"; //$NON-NLS-1$
	}

	@Override
	public IProcess getProcess() {
		return process;
	}

	@Override
	public IThread[] getThreads() throws DebugException {
		return threads;
	}

	@Override
	public boolean hasThreads() throws DebugException {
		return threads.length > 0;
	}

	@Override
	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		return breakpoint instanceof Breakpoint;
	}

	@Override
	public IDebugTarget getDebugTarget() {
		return this;
	}

	@Override
	public ILaunch getLaunch() {
		return launch;
	}

	@Override
	public String getModelIdentifier() {
		return ClonkDebugModelPresentation.ID;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canTerminate() {
		return !isTerminated();
	}

	@Override
	public boolean isTerminated() {
		return process.isTerminated();
	}

	/*+
	 * Called when the engine terminated for whatever reason.
	 */
	public void terminated() {
		synchronized (existingTargets) {
			existingTargets.remove(scenario);
		}
		try {
			terminate();
		} catch (DebugException e) {}
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
		fireTerminateEvent();
	}
	
	@Override
	public void terminate() throws DebugException {
		disconnect();
		process.terminate();
	}

	@Override
	public boolean canResume() {
		return !isTerminated() && isSuspended();
	}

	@Override
	public boolean canSuspend() {
		return !isTerminated() && !isSuspended();
	}

	@Override
	public boolean isSuspended() {
		return suspended;
	}
	
	public void send(String command, ILineReceivedListener listener) {
		if (listener != null)
			addLineReceiveListener(listener);
		synchronized (socketWriter) {
			System.out.println("Sending " + command + " to engine"); //$NON-NLS-1$ //$NON-NLS-2$
			socketWriter.println(command);
			socketWriter.flush();
		}
	}
	
	public final void send(String command) {
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
			String lost = reshuffledLines.poll();
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
	public void breakpointAdded(IBreakpoint breakpoint) {
		try {
			if (breakpoint instanceof Breakpoint) {
				Breakpoint bp = (Breakpoint) breakpoint;
				send(String.format("%s %s:%d", Commands.TOGGLEBREAKPOINT, bp.getMarker().getResource().getProjectRelativePath().toOSString(), bp.getLineNumber()-1)); //$NON-NLS-1$ 
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (delta.getAttribute(IBreakpoint.ENABLED) != null)
			breakpointAdded(breakpoint);

	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		try {
			if (breakpoint.isEnabled())
				breakpointAdded(breakpoint);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean canDisconnect() {
		return !isDisconnected();
	}

	@Override
	public void disconnect() throws DebugException {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			socket = null;
		}
		lineReceiveListeners.clear();
	}

	@Override
	public boolean isDisconnected() {
		return socket == null;
	}

	@Override
	public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
		return null;
	}

	@Override
	public boolean supportsStorageRetrieval() {
		return false;
	}

}
