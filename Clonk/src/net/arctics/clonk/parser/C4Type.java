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
	DWORD, /** @since 18.09.08 */
	REFERENCE, /** @since 23.09.08 */
	UNKNOWN; 
	
	public String toString() {
		return super.toString().toLowerCase();
	}
	
	public static C4Type makeType(String arg) {
		if (arg.equalsIgnoreCase("any")) return C4Type.ANY;
		if (arg.equalsIgnoreCase("bool")) return C4Type.BOOL;
		if (arg.equalsIgnoreCase("int")) return C4Type.INT;
		if (arg.equalsIgnoreCase("id")) return C4Type.ID;
		if (arg.equalsIgnoreCase("string")) return C4Type.STRING;
		if (arg.equalsIgnoreCase("array")) return C4Type.ARRAY;
		if (arg.equalsIgnoreCase("object")) return C4Type.OBJECT;
		if (arg.equalsIgnoreCase("dword")) return C4Type.DWORD;
		if (arg.equalsIgnoreCase("&")) return C4Type.REFERENCE;
		return C4Type.UNKNOWN;
	}
}
