package net.arctics.clonk.resource.c4group;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import net.arctics.clonk.filesystem.C4GroupFileSystem;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Represents a top-level compressed group
 *
 */
public class C4MasterGroup extends C4Group {
	
	private static final long serialVersionUID = 1L;

	private transient InputStream stream;
	private long streamPos;
	
	protected C4MasterGroup(String name, File file) {
		super(null, name, file);
	}
	
	protected C4MasterGroup(C4Group parent, String name, File file) {
		super(parent, name, file);
	}
	
	/**
	 * Creates a group from an uncompressed folder
	 * @param parent
	 * @param folder
	 */
	protected C4MasterGroup(File folder) {
		this (folder.getName(), folder);
		assert (folder.isDirectory());
	}
	
	@Override
	public InputStream requireStream() throws FileNotFoundException, IOException {
		streamPos = 0;
		if (stream != null)
			releaseStream();
		return stream = getGroupFileStream(new FileInputStream(getOrigin()));
	}
	
	@Override
	public synchronized void readFromStream(C4GroupItem whoWantsThat, long pos, StreamReadCallback callback) throws IOException {
		try {
			boolean createdStream = stream == null;
			if (createdStream)
				requireStream();
			try {
				if (stream != null) {
					if (pos > streamPos) {
						stream.skip(pos-streamPos);
						streamPos = pos;
					}
					else if (pos < streamPos) {
						releaseStream();
						requireStream();
						stream.skip(streamPos = pos);
					}
					callback.readStream(new InputStream() {

						@Override
						public int read() throws IOException {
							int result = stream.read();
							if (result != -1)
								streamPos++;
							return result;
						}

						@Override
						public int read(byte[] b) throws IOException {
							int read = stream.read(b);
							streamPos += read;
							return read;
						}

						@Override
						public int read(byte[] b, int off, int len) throws IOException {
							int read = stream.read(b, off, len);
							streamPos += read;
							return read;
						}

					});
				}
				else
					throw new IOException("C4Group.readFromStream: No stream");
			} finally {
				if (createdStream)
					releaseStream();
			}
		} catch (Exception e) {
			System.out.println("Look what you did, " + whoWantsThat.toString() + ")");
			e.printStackTrace();
		}
	}

	@Override
	public void releaseStream() throws IOException {
		if (stream == null)
			return;
		streamPos = 0;
		stream.close();
		stream = null;
//		for (C4GroupItem item : getChildren())
//			if (item instanceof C4Group)
//				((C4Group)item).releaseStream();
	}
	
	private static InputStream getGroupFileStream(final InputStream stream) throws IOException {
		return new GZIPInputStream(new InputStream() {
			private int timesRead = 0;

			@Override
			public int read() throws IOException {
				if (timesRead < 2) { // deface magic header
					timesRead++;
					int readByte = stream.read();
					if (readByte == 0x1E) return 0x1F;
					if (readByte == 0x8C) return 0x8B;
					return readByte;
				}
				return stream.read();
			}
			
			@Override
			public int read(byte[] b) throws IOException {
				return stream.read(b);
			}
			
			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				return stream.read(b, off, len);
			}
			
			@Override
			public boolean markSupported() {
				return stream.markSupported();
			}
			
			@Override
			public synchronized void mark(int readlimit) {
				stream.mark(readlimit);
			}
			
			@Override
			public synchronized void reset() throws IOException {
				stream.reset();
			}
			
			@Override
			public long skip(long n) throws IOException {
				return stream.skip(n);
			}
			
			@Override
			public void close() throws IOException {
				stream.close();
			}
			
			@Override
			public int available() throws IOException {
				return stream.available();
			}
			
		});
	}
	
	@Override
	public InputStream getStream() {
		return stream;
	}
	
	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		C4GroupFileSystem.getInstance().delete(this);
	}
	
	@Override
	protected void finalize() throws Throwable {
		releaseStream();
		super.finalize();
	}

}
