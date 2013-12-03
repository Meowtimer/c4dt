package net.arctics.clonk.util;

import java.util.Iterator;

public class ReadOnlyIterator<T> implements Iterator<T> {

	private final Iterator<T> wrapped;
	
	public ReadOnlyIterator(final Iterator<T> wrapped) {
		this.wrapped = wrapped;
	}
	
	@Override
	public boolean hasNext() {
		return wrapped.hasNext();
	}

	@Override
	public T next() {
		return wrapped.next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
