package net.arctics.clonk.util;

import java.io.Serializable;

import net.arctics.clonk.ClonkCore;

public class Pair<First, Second> implements Serializable {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	private First first;
	private Second second;

	public Pair(First first, Second second) {
		super();
		this.first = first;
		this.second = second;
	}
	public First first() {
		return first;
	}
	public void setFirst(First first) {
		this.first = first;
	}
	public Second second() {
		return second;
	}
	public void setSecond(Second second) {
		this.second = second;
	}
	@Override
	public String toString() {
		return "("+first.toString()+", "+second.toString()+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	@Override
	public boolean equals(Object other) {
		if (other instanceof Pair<?, ?>) {
			Pair<?, ?> otherPair = (Pair<?, ?>) other;
			return first.equals(otherPair.first()) && second.equals(otherPair.second());
		} else {
			return false;
		}
	}
}
