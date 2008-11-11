package net.arctics.clonk.parser;

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
	
	public String toString() {
		if (this == REFERENCE)
			return "&";
		return super.toString().toLowerCase();
	}
	
	public boolean canBeAssignedFrom(C4Type other) {
		return
			other == this ||
			this == ANY || other == ANY || this == UNKNOWN || this == REFERENCE ||
			other == UNKNOWN || other == ANY ||
			(this == BOOL && (other == INT || other == DWORD || other == OBJECT || other == STRING));
	}
	
	public static C4Type makeType(String arg) {
		// ID, Id.. all variable names
		if (arg.equals("any")) return C4Type.ANY;
		if (arg.equals("bool")) return C4Type.BOOL;
		if (arg.equals("int")) return C4Type.INT;
		if (arg.equals("id")) return C4Type.ID;
		if (arg.equals("string")) return C4Type.STRING;
		if (arg.equals("array")) return C4Type.ARRAY;
		if (arg.equals("object")) return C4Type.OBJECT;
		if (arg.equals("dword")) return C4Type.DWORD;
		if (arg.equals("&")) return C4Type.REFERENCE;
		return C4Type.UNKNOWN;
	}
}
