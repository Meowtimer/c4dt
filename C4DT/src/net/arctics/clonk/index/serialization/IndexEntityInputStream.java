package net.arctics.clonk.index.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import net.arctics.clonk.index.IDeserializationResolvable;
import net.arctics.clonk.index.Index;

public class IndexEntityInputStream extends ObjectInputStream {
	private Index index;
	public IndexEntityInputStream(Index index, InputStream input) throws IOException {
		super(input);
		this.index = index;
		enableResolveObject(true);
	}
	@Override
	protected Object resolveObject(Object obj) throws IOException {
		if (index == null && obj instanceof Index)
			index = (Index)obj;
		else if (obj instanceof IDeserializationResolvable)
			return ((IDeserializationResolvable)obj).resolve(index);
		else
			return super.resolveObject(obj);
		return obj;
	}
}
