package net.arctics.clonk.index;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

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
		else if (obj instanceof ISerializationResolvable)
			return ((ISerializationResolvable)obj).resolve(index);
		else
			return super.resolveObject(obj);
		return obj;
	}
}
