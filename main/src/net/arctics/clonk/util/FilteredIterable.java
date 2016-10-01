package net.arctics.clonk.util;

import java.util.Iterator;

/**
 * Iterable adapter that filters out items obtained from a base iterable.
 * @author madeen
 *
 * @param <T> Type the objects returned by this iterable are at least of.
 */
public class FilteredIterable<T, B> implements Iterable<T> {
	private final Iterable<? extends B> base;
	private final Class<? extends T> cls;
	private final boolean callRemove;
	/**
	 * Create a new FilteredIterable.
	 * @param cls Class that needs to be passed in order for runtime type information to be available...
	 * @param base Base iterable that will be queried.
	 * @param callRemove Whether to call remove on the base iterator if an item has been determined to be invalid.
	 */
	public FilteredIterable(final Class<? extends T> cls, final Iterable<? extends B> base, final boolean callRemove) {
		this.base = base;
		this.cls = cls;
		this.callRemove = callRemove;
	}
	/**
	 * Decide whether the item is still valid.
	 * @param item The item to test
	 * @return Whether it's still valid or not.
	 */
	protected boolean stillValid(final B item) {
		return true;
	}

	@SuppressWarnings("unchecked")
	protected T map(final B original) {
		return cls.isAssignableFrom(original.getClass()) ? (T) original : null;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private final Iterator<? extends B> baseIterator = base.iterator();
			private boolean calledHasNext = false;
			private T next;
			@Override
			public boolean hasNext() {
				if (calledHasNext) {
					return next != null;
				} else {
					calledHasNext = true;
					while (baseIterator.hasNext()) {
						final B n = baseIterator.next();
						T mapped;
						if (n != null && stillValid(n) && (mapped = map(n)) != null) {
							next = mapped;
							return true;
						} else // not deemed valid? remove!
						if (callRemove) {
							baseIterator.remove();
						}
					}
					next = null;
					return false;
				}
			}
			@Override
			public T next() {
				calledHasNext = false;
				return next;
			}
			@Override
			public void remove() {
				baseIterator.remove();
			}
		};
	}
}
