package net.arctics.clonk.util;

@FunctionalInterface
public interface Sink<T> {
	public enum Decision {
		PurgeItem,
		Continue,
		AbortIteration
	}
	static ThreadLocal<Decision> decision = new ThreadLocal<>();
	public default Decision elutriate(final T item) {
		if (item == null)
			return Decision.PurgeItem;
		else if (filter(item)) {
			decision.set(Decision.Continue);
			receivedObject(item);
			return decision.get();
		}
		else
			return Decision.Continue;
	}
	public default void decision(final Decision decision) {
		Sink.decision.set(decision);
	}
	public abstract void receivedObject(T item);
	public default boolean filter(final T item) { return true; }
}
