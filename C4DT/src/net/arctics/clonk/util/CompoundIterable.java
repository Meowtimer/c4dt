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
		while (curIterator != null) {
			// current iterator still has more?
			if (curIterator.hasNext())
				return true;
			else
				curIterator = null;
			// look for next with something in store
			for (++iterIndex; iterIndex < subIterables.length; iterIndex++) {
				if (subIterables[iterIndex] != null) {
					curIterator = subIterables[iterIndex].iterator();
					break;
				}
			}
		}
		return false;
	}
 
	public T next() {
		T next = curIterator.next();
		if (next == null)
			System.out.println("CompoundIterator complains about its underling " + curIterator + ", it returns null"); //$NON-NLS-1$ //$NON-NLS-2$
		return next;
	}

	public void remove() {	
	}

}
