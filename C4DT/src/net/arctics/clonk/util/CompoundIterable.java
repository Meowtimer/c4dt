package net.arctics.clonk.util;

import java.util.Iterator;

public class CompoundIterable<T> implements Iterable<T>, Iterator<T> {

	private Iterable<? extends T>[] subIterables;
	private Iterator<? extends T> curIterator;
	private int iterIndex;
	
	public Iterator<T> iterator() {
		return this; 
	}
	
	public CompoundIterable(Iterable<? extends T>... subIterables) {
		this.subIterables = subIterables;
		for (iterIndex = 0; iterIndex < subIterables.length && subIterables[iterIndex] == null; iterIndex++);
		if (iterIndex < subIterables.length)
			curIterator = subIterables[iterIndex].iterator();
	}

	public boolean hasNext() {
		for (;
			curIterator != null && !curIterator.hasNext() && ++iterIndex < subIterables.length;
			curIterator = subIterables[iterIndex] != null ? subIterables[iterIndex].iterator() : curIterator
		);
		return curIterator != null && curIterator.hasNext();
	}
 
	public T next() {
		return curIterator.next();
	}

	public void remove() {	
	}

}
