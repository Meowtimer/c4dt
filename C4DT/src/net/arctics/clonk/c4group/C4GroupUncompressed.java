package net.arctics.clonk.c4group;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import net.arctics.clonk.Core;
import net.arctics.clonk.util.ITreeNode;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class C4GroupUncompressed extends C4Group {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public C4GroupUncompressed(final C4Group parent, final String name, final File file) {
		super(parent, name, file);
	}
	
	@Override
	public InputStream stream() {
		return null;
	}
	
	@Override
	public void releaseStream() throws IOException {
	}
	
	@Override
	public InputStream requireStream() throws FileNotFoundException, IOException {
		return null;
	}
	
	@Override
	public synchronized void readFromStream(final C4GroupItem whoWantsThat, final long pos, final StreamReadCallback callback) throws IOException {
		// create stream from uncompressed file
		final IPath path = new Path(origin().toString()).append(ITreeNode.Default.relativePath(whoWantsThat, this));
		final File fileToReadFrom = path.toFile();
		if (fileToReadFrom.isFile()) {
			final FileInputStream stream = new FileInputStream(fileToReadFrom);
			try {
				callback.readStream(stream);
			} finally {
				stream.close();
			}
		}
	};
	
	@Override
	public int baseOffset() {
		return 0;
	}

}
