package net.arctics.clonk.parser.landscapescript;

public enum Operator {
	Or('|'),
	And('&'),
	XOr('^');
	
	private final char c;
	
	Operator(char c) {
		this.c = c;
	}
	
	@Override
	public String toString() {
		return String.valueOf(c);
	}
	
	public static Operator valueOf(char c) {
		for (Operator o : values())
			if (o.c == c)
				return o;
		return null;
	}
}