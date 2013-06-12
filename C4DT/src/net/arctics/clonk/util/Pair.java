package net.arctics.clonk.util;

import static net.arctics.clonk.util.Utilities.eq;

import java.io.Serializable;

import net.arctics.clonk.Core;

public class Pair<First, Second> implements Serializable, Cloneable {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private First first;
	private Second second;

	public Pair(First first, Second second) {
		super();
		this.first = first;
		this.second = second;
	}
	public final First first() {
		return first;
	}
	public final void setFirst(First first) {
		this.first = first;
	}
	public final Second second() {
		return second;
	}
	public final void setSecond(Second second) {
		this.second = second;
	}
	@Override
	public String toString() {
		final Object first = this.first != null ? this.first : "<null>";
		final Object second = this.second != null ? this.second : "<null>";
		return "("+first.toString()+", "+second.toString()+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	@Override
	public boolean equals(Object other) {
		if (other instanceof Pair<?, ?>) {
			final Pair<?, ?> otherPair = (Pair<?, ?>) other;
			return eq(first, otherPair.first) && eq(second, otherPair.second);
		} else
			return false;
	}
	@Override
	public int hashCode() {
		return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode());
	}
	@SuppressWarnings("unchecked")
	@Override
	public Pair<First, Second> clone() throws CloneNotSupportedException {
		return (Pair<First, Second>) super.clone();
	}

	public static <F, S> Pair<F, S> pair(F first, S second) {
		return new Pair<F, S>(first, second);
	}
}
