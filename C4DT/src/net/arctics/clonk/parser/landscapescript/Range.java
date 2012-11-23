package net.arctics.clonk.parser.landscapescript;

import static net.arctics.clonk.mapcreator.GlobalFunctions.Random;

import java.io.Serializable;

import net.arctics.clonk.Core;

public final class Range implements Serializable {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private final NumVal lo, hi;
	private final int evaluated;
	
	public Range(NumVal lo) { this(lo, lo); }
	public Range(Unit unit, int val) { this(new NumVal(unit, val)); }

	public Range(NumVal lo, NumVal hi) {
		super();
		this.lo = lo;
		this.hi = hi;
		this.evaluated = lo.value() + (hi != null ? Random(hi.value()-lo.value()) : 0);
	}
	
	public int evaluate(int relative_to) {
		switch (lo.unit()) {
		case Percent:
			return evaluated * relative_to / 100;
		case Pixels:
			return evaluated;
		default:
			return 0;
		}
	}

	public NumVal lo() { return lo; }
	public NumVal hi() { return hi; }
	public int evaluated() { return evaluated; }
	
	@Override
	public String toString() {
		if (lo != null && hi != null)
			return lo.toString() + " - " + hi.toString(); //$NON-NLS-1$
		else if (lo != null)
			return lo.toString();
		else
			return "<Empty Range>"; //$NON-NLS-1$
	}
}