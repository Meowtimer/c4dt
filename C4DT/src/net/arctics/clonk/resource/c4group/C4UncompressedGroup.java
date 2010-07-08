package net.arctics.clonk.resource.c4group;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.util.ITreeNode;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class C4UncompressedGroup extends C4Group {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public C4UncompressedGroup(C4Group parent, String name, File file) {
		super(parent, name, file);
	}
	
	@Override
	public InputStream getStream() {
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
		IPath path = new Path(getOrigin().toString()).append(ITreeNode.Default.relativePath(whoWantsThat, this));
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
