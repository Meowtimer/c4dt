package net.arctics.clonk.util;

public abstract class Sink<T> {
	public enum Decision {
		PurgeItem,
		Continue,
		AbortIteration
	}
	private Decision decision;
	public final Decision elutriate(T item) {
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
	public final void decision(Decision decision) {
		this.decision = decision;
	}
	public abstract void receivedObject(T item);
	public boolean filter(T item) { return true; }
}
