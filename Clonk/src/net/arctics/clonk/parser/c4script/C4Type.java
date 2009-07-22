package net.arctics.clonk.parser.c4script;

/**
 * The engine predefined variable types.
 * @author ZokRadonh
 *
 */
public enum C4Type {
	UNKNOWN,
	
	ANY,
	BOOL,
	INT,
	ID,
	STRING,
	ARRAY,
	OBJECT,
	DWORD, 
	REFERENCE, 
	PROPLIST; 
	
	@Override
	public String toString() {
		return toString(false);
	}
	
	public String toString(boolean special) {
		if (!special && this == REFERENCE)
			return "&";
		return super.toString().toLowerCase();
	}
	
	public boolean canBeAssignedFrom(C4Type other) {
		return
			other == this ||
			this == ANY || this == UNKNOWN || this == REFERENCE || this == BOOL ||
			other == UNKNOWN || other == ANY || other == REFERENCE ||
			(this == INT && (other == DWORD || other == BOOL)) ||
			(this == DWORD && (other == INT || other == BOOL));
	}
	
	public static C4Type makeType(String arg) {
		return makeType(arg, false);
	}
	
	private static final C4Type[] officialTypes = new C4Type[] {BOOL, INT, ID, STRING, ARRAY, OBJECT, PROPLIST};
	
	public static C4Type makeType(String arg, boolean allowSpecial) {
		for (C4Type t : officialTypes)
			if (t.name().equalsIgnoreCase(arg))
				return t;
		if (allowSpecial) {
			if (arg.equals("dword"))
				return C4Type.DWORD;
			if (arg.equals("any"))
				return C4Type.ANY;
		}
		if (arg.equals("&") || (allowSpecial && arg.equals("reference")))
			return C4Type.REFERENCE;
		return C4Type.UNKNOWN;
	}

}
