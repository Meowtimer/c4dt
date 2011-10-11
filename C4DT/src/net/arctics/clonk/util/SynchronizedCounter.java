package net.arctics.clonk.util;

public class SynchronizedCounter {
	private int value;
	public SynchronizedCounter(int value) {
		this.value = value;
	}
	public synchronized int increment(int by) {
		return this.value += by;
	}
	public synchronized int decrement(int by) {
		return this.value -= by;
	}
	public synchronized int decrement() {
		return --this.value;
	}
	public synchronized int increment() {
		return ++this.value;
	}
	public synchronized int value() {
		return this.value;
	}
}
