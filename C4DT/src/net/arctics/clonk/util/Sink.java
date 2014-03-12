package net.arctics.clonk.util;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@FunctionalInterface
public interface Sink<T> {
	public enum Decision {
		PurgeItem,
		Continue,
		AbortIteration
	}
	public default Decision elutriate(final T item) {
		if (item == null)
			return Decision.PurgeItem;
		else if (filter(item)) {
			decision(Decision.Continue);
			receivedObject(item);
			return decision();
		}
		else
			return Decision.Continue;
	}
	public default Decision decision() { return Decision.Continue; }
	public default void decision(final Decision decision) { throw new NotImplementedException(); }
	public void receivedObject(T item);
	public default boolean filter(final T item) { return true; }
	public abstract class Decisive<T> implements Sink<T> {
		private Decision decision;
		@Override
		public void decision(Decision decision) { this.decision = decision; }
		@Override
		public Decision decision() { return decision; }
	}
}
