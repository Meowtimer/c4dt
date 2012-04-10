package net.arctics.clonk.util;

public abstract class Sink<T> {
	public final void elutriated(T item) {
		if (item == null)
			System.out.println("What have we got here?");
		else if (filter(item))
			receivedObject(item);
	}
	public abstract void receivedObject(T item);
	public boolean filter(T item) {
		return true;
	}
}
