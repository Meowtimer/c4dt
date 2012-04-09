package net.arctics.clonk.resource.c4group;

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

	public C4GroupUncompressed(C4Group parent, String name, File file) {
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
	public synchronized void readFromStream(C4GroupItem whoWantsThat, long pos, StreamReadCallback callback) throws IOException {
		// create stream from uncompressed file
		IPath path = new Path(origin().toString()).append(ITreeNode.Default.relativePath(whoWantsThat, this));
		File fileToReadFrom = path.toFile();
		if (fileToReadFrom.isFile()) {
			FileInputStream stream = new FileInputStream(fileToReadFrom);
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
