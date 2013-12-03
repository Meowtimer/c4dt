package net.arctics.clonk.index.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import net.arctics.clonk.index.IDeserializationResolvable;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;

public class IndexEntityInputStream extends ObjectInputStream {
	private Index index;
	private final IndexEntity entity;
	public IndexEntityInputStream(final Index index, final IndexEntity entity, final InputStream input) throws IOException {
		super(input);
		this.index = index;
		this.entity = entity;
		enableResolveObject(true);
	}
	@Override
	protected Object resolveObject(final Object obj) throws IOException {
		if (obj instanceof String)
			return ((String)obj).intern();
		if (index == null && obj instanceof Index)
			index = (Index)obj;
		else if (obj instanceof IDeserializationResolvable)
			return ((IDeserializationResolvable)obj).resolve(index, entity);
		else
			return super.resolveObject(obj);
		return obj;
	}
}
