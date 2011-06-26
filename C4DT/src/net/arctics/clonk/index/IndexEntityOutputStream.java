package net.arctics.clonk.index;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class IndexEntityOutputStream extends ObjectOutputStream {
	private Index index;

	public IndexEntityOutputStream(Index index, OutputStream output) throws IOException {
		super(output);
		this.index = index;
		enableReplaceObject(true);
	}

	@Override
	protected Object replaceObject(Object obj) throws IOException {
		if (obj instanceof IndexEntity)
			return index.getSaveReplacementForEntity((IndexEntity)obj);
		else if (obj instanceof String)
			return ((String)obj).intern();
		else
			return super.replaceObject(obj);
	}

}
