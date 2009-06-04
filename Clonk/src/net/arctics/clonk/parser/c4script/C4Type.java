package net.arctics.clonk.parser.c4script;

/**
 * The engine predefined variable types.
 * @author ZokRadonh
 *
 */
public enum C4Type {
	ANY,
	BOOL,
	INT,
	ID,
	STRING,
	ARRAY,
	OBJECT,
	/** @since 18.09.08 */
	DWORD, 
	/** @since 23.09.08 */
	REFERENCE, 
	UNKNOWN; 
	
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
	
	public static C4Type makeType(String arg, boolean allowSpecial) {
		// ID, Id.. all variable names
		//if (arg.equals("any")) return C4Type.ANY; engine does not like that
		if (arg.equals("bool")) return C4Type.BOOL;
		if (arg.equals("int")) return C4Type.INT;
		if (arg.equals("id")) return C4Type.ID;
		if (arg.equals("string")) return C4Type.STRING;
		if (arg.equals("array")) return C4Type.ARRAY;
		if (arg.equals("object")) return C4Type.OBJECT;
		if (allowSpecial && arg.equals("dword")) return C4Type.DWORD;
		if (arg.equals("&") || (allowSpecial && arg.equals("reference"))) return C4Type.REFERENCE;
		return C4Type.UNKNOWN;
	}

}
