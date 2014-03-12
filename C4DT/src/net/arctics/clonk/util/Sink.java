package net.arctics.clonk.util;


@FunctionalInterface
public interface Sink<T> {
	enum Decision {
		PurgeItem,
		Continue,
		AbortIteration
	}
	default Decision elutriate(final T item) {
		if (item == null)
			return Decision.PurgeItem;
		else if (filter(item)) {
			receive(item);
			return decide(item);
		}
		else
			return Decision.Continue;
	}
	void receive(T item);
	default Decision decide(T item) { return Decision.Continue; }
	default boolean filter(final T item) { return true; }
}
