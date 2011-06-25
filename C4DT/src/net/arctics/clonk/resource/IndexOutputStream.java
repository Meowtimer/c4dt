package net.arctics.clonk.resource;

import java.io.IOException;
import java.io.OutputStream;

import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntityOutputStream;

public class IndexOutputStream extends IndexEntityOutputStream {
	
	public IndexOutputStream(Index index, OutputStream output) throws IOException {
		super(index, output);
	}

}
