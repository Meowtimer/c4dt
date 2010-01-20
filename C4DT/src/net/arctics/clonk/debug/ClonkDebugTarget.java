package net.arctics.clonk.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.arctics.clonk.ui.debug.ClonkDebugModelPresentation;
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

public class ClonkDebugTarget extends ClonkDebugElement implements IDebugTarget {

	public static class Commands {
		public static final String RESUME = "GO"; //$NON-NLS-1$
		public static final String SUSPEND = "STP"; //$NON-NLS-1$
		public static final String STEPOVER = "STO";
		public static final String STEPRETURN = "STR";
		public static final String QUITSESSION = "BYE";
		public static final String STACKTRACE = "STA";
	}
	
	public static final int CONNECTION_ATTEMPT_WAITTIME = 2000;
	
	private ILaunch launch;
	private IProcess process;
	private ClonkDebugThread thread;
	private IThread[] threads;
	private Socket socket;
	private PrintWriter socketWriter;
	private BufferedReader socketReader;
	private boolean suspended;
	private IResource scenario;
	
	public PrintWriter getSocketWriter() {
		return socketWriter;
	}

	public BufferedReader getSocketReader() {
		return socketReader;
	}
	
	private class EventDispatchJob extends Job {
		
		public EventDispatchJob(String name) {
			super(name);
		}
		
		private List<String> stackTrace = new ArrayList<String>(10);
		
		private final String receive() throws IOException {
			String r = socketReader.readLine();
			if (r != null) {
				if (r.charAt(r.length()-1) == 0)
					r = r.substring(0, r.length()-1);
				System.out.println("Got line from Clonk: " + r); //$NON-NLS-1$
			}
			return r;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String event = ""; //$NON-NLS-1$
			while (!isTerminated() && event != null) {
				try {
					event = receive();
					if (event != null && event.length() > 0) {
						if (event.startsWith("POS")) { //$NON-NLS-1$
							String sourcePath = event.substring(4, event.length());
							stackTrace.clear();
							stackTrace.add(sourcePath);

							send("SST");
							while (!isTerminated() && event != null) {
								event = receive();
								if (event != null && event.length() > 0) {
									if (event.equals("EST")) {
										break;
									}
									else if (event.startsWith("AT ")) {
										if (stackTrace.size() > 512) {
											System.out.println("Runaway stacktrace");
											break;
										}
										else {
											stackTrace.add(event.substring("AT ".length()));
										}
									}
									else
										break;
								}
							}

							stoppedWithStackTrace(stackTrace);
							
							try {
								if (thread.getTopStackFrame() != null && thread.getTopStackFrame().getVariables() != null) {
									ClonkDebugVariable[] varArray = ((ClonkDebugStackFrame)thread.getTopStackFrame()).getVariables();
									Map<String, ClonkDebugVariable> vars = new HashMap<String, ClonkDebugVariable>(varArray.length);
									for (ClonkDebugVariable var : varArray)
										vars.put(var.getName(), var);
									for (ClonkDebugVariable var : vars.values()) {
										send("VAR " + var.getName());
									}
									while (!isTerminated() && event != null && !vars.isEmpty()) {
										event = receive();
										if (event != null && event.length() > 0) {
											if (event.startsWith("VAR ")) {
												event = event.substring("VAR ".length());
												String[] parts = event.split("=");
												if (parts != null && parts.length == 2) {
													ClonkDebugVariable var = vars.get(parts[0]);
													if (var != null) {
														var.getValue().setValue(parts[1]);
														vars.remove(parts[0]);
													}
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
						}
					}
				} catch (IOException e) {
					//e.printStackTrace();
					terminated();
					break;
				}
			}
			return Status.OK_STATUS;
		}
		
	}
	
	private class ConnectionJob extends Job {

		private int port;
		
		public ConnectionJob(String name, int port) {
			super(name);
			this.port = port;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			boolean success = false;
			// try several times to give the engine a chance to load
			for (int attempts = 0; attempts < 30 && !monitor.isCanceled(); attempts++) {
				Socket socket;
				try {
					socket = new Socket("localhost", port); //$NON-NLS-1$
				} catch (Exception e) {
					if (e instanceof UnknownHostException || e instanceof IOException) {
						try {Thread.sleep(CONNECTION_ATTEMPT_WAITTIME);} catch (InterruptedException interrupt) {}
						continue;
					}
					else {
						e.printStackTrace();
						return Status.CANCEL_STATUS;
					}
				}
				PrintWriter socketWriter = null;
				BufferedReader socketReader = null;
				try {
					socketWriter = new PrintWriter(socket.getOutputStream());
					socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				} catch (IOException e) {
					e.printStackTrace();
				}
				setConnectionObjects(socket, socketWriter, socketReader);
				success = true;
				break;
			}
			if (!success)
				System.out.println("Clonk Debugger: Connecting to engine failed"); //$NON-NLS-1$
			else
				System.out.println("Clonk Debugger: Connected successfully!"); //$NON-NLS-1$
			return Status.OK_STATUS;
		}
		
	}
	
	synchronized private void setConnectionObjects(Socket socket, PrintWriter socketWriter, BufferedReader socketReader_) {
		this.socket = socket;
		this.socketWriter = socketWriter;
		this.socketReader = socketReader_;
		
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
		
		send(""); //$NON-NLS-1$
		send("STP"); // suspend in order to set breakpoints
		setBreakpoints();
		send("GO"); // go!
		
		new EventDispatchJob("Clonk Debugger Event Dispatch").schedule(); //$NON-NLS-1$
	}
	
	private void setBreakpoints() {
		IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(ClonkDebugModelPresentation.ID);
		for (IBreakpoint b : breakpoints) {
			breakpointAdded(b);
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

	public ClonkDebugTarget(ILaunch launch, IProcess process, int port, IResource scenario) throws Exception {

		super(null);

		this.launch = launch;
		this.process = process;
		this.thread = new ClonkDebugThread(this);
		this.threads = new IThread[] {thread};
		this.scenario = scenario;

		new ConnectionJob("Clonk Debugger Connection Job", port).schedule(); //$NON-NLS-1$

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
		return breakpoint instanceof ClonkDebugLineBreakpoint;
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

	public void terminated() {
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
	
	public synchronized void send(String command) {
		System.out.println("Sending " + command + " to engine");
		socketWriter.println(command);
		socketWriter.flush();
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
			if (breakpoint instanceof ClonkDebugLineBreakpoint) {
				ClonkDebugLineBreakpoint bp = (ClonkDebugLineBreakpoint) breakpoint;
				send("TBR " + bp.getMarker().getResource().getProjectRelativePath().toOSString() + ":" + bp.getLineNumber());
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		// TODO Auto-generated method stub

	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		breakpointAdded(breakpoint);
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
