package net.arctics.clonk.util;

import org.eclipse.core.resources.IResourceVisitor;

public abstract class ObjectFinderVisitor<T> implements IResourceVisitor {

	protected T result;
	
	public T result() {
		return result;
	}
	
	public void reset() {
		result = null;
	}

}
