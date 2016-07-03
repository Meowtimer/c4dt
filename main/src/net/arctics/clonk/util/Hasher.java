package net.arctics.clonk.util;

public final class Hasher {
	
	int result = 17;

	public Hasher in(int c) {
		result = 31 * result + c;
		return this;
	}

	public Hasher in(Object o) {
		return in(o != null ? o.hashCode() : 0);
	}

	public Hasher in(Object[] a) {
		for (final Object o : a) {
			in(o);
		}
		return this;
	}

	public int finish() { return result; }

}