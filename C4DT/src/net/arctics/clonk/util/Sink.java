package net.arctics.clonk.util;

public abstract class Sink<T> {
	public enum Decision {
		PurgeItem,
		Continue,
		AbortIteration
	}
	private Decision decision;
	public final Decision elutriate(final T item) {
		if (item == null)
			return Decision.PurgeItem;
		else if (filter(item)) {
			decision = Decision.Continue;
			receivedObject(item);
			return decision;
		}
		else
			return Decision.Continue;
	}
	public final void decision(final Decision decision) {
		this.decision = decision;
	}
	public abstract void receivedObject(T item);
	public boolean filter(final T item) { return true; }
}
