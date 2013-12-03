package net.arctics.clonk.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

class ConnectionJob extends Job {
	private final Target target;
	private final int port;
	
	public ConnectionJob(final Target target, final String name, final int port) {
		super(name);
		this.target = target;
		this.port = port;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		boolean success = false;
		// try several times to give the engine a chance to load
		for (int attempts = 0; attempts < 30 && !monitor.isCanceled(); attempts++) {
			Socket socket;
			try {
				socket = new Socket("localhost", port); //$NON-NLS-1$
			} catch (final Exception e) {
				if (e instanceof UnknownHostException || e instanceof IOException) {
					try {Thread.sleep(Target.CONNECTION_ATTEMPT_WAITTIME);} catch (final InterruptedException interrupt) {}
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
			} catch (final IOException e) {
				e.printStackTrace();
			}
			this.target.setConnectionObjects(socket, socketWriter, socketReader);
			success = true;
			break;
		}
		if (!success) {
			System.out.println("Clonk Debugger: Connecting to engine failed"); //$NON-NLS-1$
			this.target.terminated();
			return Status.CANCEL_STATUS;
		} else {
			System.out.println("Clonk Debugger: Connected successfully!"); //$NON-NLS-1$
			return Status.OK_STATUS;
		}
	}
	
}