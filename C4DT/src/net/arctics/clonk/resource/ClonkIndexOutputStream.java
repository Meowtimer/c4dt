package net.arctics.clonk.resource;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.c4script.ImportedObject;

public class ClonkIndexOutputStream extends ObjectOutputStream {

	private ClonkIndex index;
	
	public ClonkIndexOutputStream(ClonkIndex index, OutputStream output) throws IOException {
		super(output);
		this.index = index;
		enableReplaceObject(true);
	}
	
	@Override
	protected Object replaceObject(Object obj) throws IOException {
		if (obj instanceof C4Object) {
			return ImportedObject.getSerializableType(index, (C4Object)obj);
		}
		return obj;
	}

}
