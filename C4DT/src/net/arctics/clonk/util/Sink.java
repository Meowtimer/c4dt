package net.arctics.clonk.util;

public interface Sink<T> {
	void receivedObject(T item);
}
