package net.arctics.clonk.util;

public abstract class Sink<T> {
	public enum Decision {
		Purge,
		Keep
	}
	public final Decision elutriate(T item) {
		if (item == null)
			return Decision.Purge;
		else if (filter(item))
			receivedObject(item);
		return Decision.Keep;
	}
	public abstract void receivedObject(T item);
	public boolean filter(T item) {
		return true;
	}
}
