package net.arctics.clonk.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CompoundIterable<T> implements Iterable<T>, Iterator<T> {

	private List<Iterable<? extends T>> subIterables;
	private Iterator<? extends T> curIterator;
	private int iterIndex;
	
	public Iterator<T> iterator() {
		return this; 
	}
	
	public CompoundIterable(List<Iterable<? extends T>> subIterables) {
		this.subIterables = subIterables;
		for (iterIndex = 0; iterIndex < subIterables.size() && subIterables.get(iterIndex) == null; iterIndex++);
		if (iterIndex < subIterables.size())
			curIterator = subIterables.get(iterIndex).iterator();
	}
	
	public CompoundIterable(Iterable<? extends T>... subIterables) {
		this(Arrays.asList(subIterables));
	}

	public boolean hasNext() {
		while (curIterator != null) {
			// current iterator still has more?
			if (curIterator.hasNext())
				return true;
			else
				curIterator = null;
			// look for next with something in store
			for (++iterIndex; iterIndex < subIterables.size(); iterIndex++) {
				if (subIterables.get(iterIndex) != null) {
					curIterator = subIterables.get(iterIndex).iterator();
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
