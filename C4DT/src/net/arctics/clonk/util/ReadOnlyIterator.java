package net.arctics.clonk.util;

import java.util.Iterator;

public class ReadOnlyIterator<T> implements Iterator<T> {

	private Iterator<T> wrapped;
	
	public ReadOnlyIterator(Iterator<T> wrapped) {
		this.wrapped = wrapped;
	}
	
	public boolean hasNext() {
		return wrapped.hasNext();
	}

	public T next() {
		return wrapped.next();
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

}
