package net.arctics.clonk.landscapescript;

public enum Operator {
	Or('|'),
	And('&'),
	XOr('^');
	
	private final char c;
	
	Operator(final char c) {
		this.c = c;
	}
	
	@Override
	public String toString() {
		return String.valueOf(c);
	}
	
	public static Operator valueOf(final char c) {
		for (final Operator o : values())
			if (o.c == c)
				return o;
		return null;
	}
}