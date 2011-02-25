package net.arctics.clonk.util;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;

public class WeakListenerManager<T> {
	protected LinkedList<WeakReference<? extends T>> listeners = new LinkedList<WeakReference<? extends T>>();
	public void addListener(T listener) {
		purge();
		listeners.add(new WeakReference<T>(listener));
	}
	protected void purge() {
		Iterator<WeakReference<? extends T>> it = listeners.iterator();
		while (it.hasNext()) {
			WeakReference<? extends T> ref = it.next();
			if (ref.get() == null)
				it.remove();
		}
	}
}
