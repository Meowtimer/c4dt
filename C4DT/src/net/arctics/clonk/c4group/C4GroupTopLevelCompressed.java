package net.arctics.clonk.c4group;

import static net.arctics.clonk.util.Utilities.attempt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import net.arctics.clonk.Core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Represents a top-level compressed C4Group.
 */
public class C4GroupTopLevelCompressed extends C4Group {

	private static final class SeekableStream extends FilterInputStream {
		private static GZIPInputStream makeGZIPStream(File file) {
			return attempt(() -> new GZIPInputStream(new FilterInputStream(new FileInputStream(file)) {
				private int timesRead = 0;
				@Override
				public int read() throws IOException {
					if (timesRead < 2) { // deface magic header
						timesRead++;
						final int readByte = in.read();
						if (readByte == 0x1E) return 0x1F;
						if (readByte == 0x8C) return 0x8B;
						return readByte;
					}
					return in.read();
				}
			}), IOException.class, Exception::printStackTrace);
		}
		private long streamPos;
		private final File file;
		private SeekableStream(File file) {
			super(makeGZIPStream(file));
			this.file = file;
			this.streamPos = 0;
		}
		@Override
		public synchronized void reset() throws IOException {
			in.close();
			in = makeGZIPStream(file);
		}
		@Override
		public int read() throws IOException {
			final int result = super.read();
			if (result != -1)
				streamPos++;
			return result;
		}
		@Override
		public int read(final byte[] b) throws IOException {
			final int read = super.read(b);
			streamPos += read;
			return read;
		}
		@Override
		public int read(final byte[] b, final int off, final int len) throws IOException {
			final int read = super.read(b, off, len);
			streamPos += read;
			return read;
		}
		public void seek(long pos) throws IOException {
			if (pos > streamPos) {
				in.skip(pos-streamPos);
				streamPos = pos;
			}
			else if (pos < streamPos) {
				reset();
				in.skip(streamPos = pos);
			}
		}
	}

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private transient SeekableStream stream;

	public C4GroupTopLevelCompressed(final String name, final File file) {
		super(null, name, file);
	}

	protected C4GroupTopLevelCompressed(final C4Group parent, final String name, final File file) {
		super(parent, name, file);
	}

	/**
	 * Creates a group from an uncompressed folder
	 * @param parent
	 * @param folder
	 */
	protected C4GroupTopLevelCompressed(final File folder) {
		this (folder.getName(), folder);
		assert (folder.isDirectory());
	}

	@Override
	public InputStream requireStream() throws FileNotFoundException, IOException {
		if (stream != null)
			releaseStream();
		stream = new SeekableStream(origin());
		if (stream == null) {
			System.out.println("Failed to create stream for " + origin());
			return null;
		} else
			return stream;
	}

	@Override
	public synchronized void readFromStream(final C4GroupItem whoWantsThat, final long pos, final StreamReadCallback callback) throws IOException {
		try {
			final boolean createdStream = stream == null;
			if (createdStream)
				requireStream();
			try {
				if (stream != null) {
					stream.seek(pos);
					callback.readStream(stream);
				}
				else
					throw new IOException("C4Group.readFromStream: No stream"); //$NON-NLS-1$
			} finally {
				if (createdStream)
					releaseStream();
			}
		} catch (final Exception e) {
			System.out.println("Look what you did, " + whoWantsThat.toString() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			e.printStackTrace();
		}
	}

	@Override
	public void releaseStream() throws IOException {
		if (stream == null)
			return;
		stream.close();
		stream = null;
	}

	@Override
	public InputStream stream() {
		return stream;
	}

	@Override
	public void delete(final int options, final IProgressMonitor monitor) throws CoreException {
		C4GroupFileSystem.instance().removeGroupFromRegistry(this);
	}

	@Override
	protected void finalize() throws Throwable {
		releaseStream();
		super.finalize();
	}

	@Override
	public boolean existsOnDisk() {
		final File origin = origin();
		return origin != null && origin.exists();
	}

}
