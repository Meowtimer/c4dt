package net.arctics.clonk.debug;

import static net.arctics.clonk.util.Utilities.tri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.stream.Stream;

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

	static class ConnectionObjects {
		public PrintWriter socketWriter;
		public BufferedReader socketReader;
		public ConnectionObjects(PrintWriter sockerWriter, BufferedReader socketReader) {
			super();
			this.socketWriter = sockerWriter;
			this.socketReader = socketReader;
		}
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		// try several times to give the engine a chance to load
		final Socket socket = Stream
			.generate(() -> tri(
				() -> monitor.isCanceled() ? null : new Socket("localhost", port),
				Exception.class,
				e -> {
					if (e instanceof UnknownHostException || e instanceof IOException)
						try {
							Thread.sleep(Target.CONNECTION_ATTEMPT_WAITTIME);
						} catch (final InterruptedException interrupt) {}
				}
			))
			.limit(30)
			.filter(s -> s != null)
			.findFirst().orElse(null);

		final ConnectionObjects objs = socket != null ? tri(() ->
			new ConnectionObjects(
				new PrintWriter(socket.getOutputStream()),
				new BufferedReader(new InputStreamReader(socket.getInputStream())
			)
		), IOException.class, e -> e.printStackTrace()) : null;

		if (objs != null) {
			this.target.setConnectionObjects(socket, objs.socketWriter, objs.socketReader);
			System.out.println("Clonk Debugger: Connected successfully!"); //$NON-NLS-1$
			return Status.OK_STATUS;
		} else {
			System.out.println("Clonk Debugger: Connecting to engine failed"); //$NON-NLS-1$
			this.target.terminated();
			return Status.CANCEL_STATUS;
		}
	}

}