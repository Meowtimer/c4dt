package net.arctics.clonk.parser.c4script;

import java.lang.reflect.Array;
import java.util.Map;

import net.arctics.clonk.parser.C4ID;

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
	//DWORD, 
	REFERENCE, 
	PROPLIST; 
	
	@Override
	public String toString() {
		return toString(false);
	}
	
	public String toString(boolean special) {
		if (!special && this == REFERENCE)
			return "&"; //$NON-NLS-1$
		return super.toString().toLowerCase();
	}
	
	public boolean canBeAssignedFrom(C4Type other) {
		return
			other == this ||
			this == ANY || this == UNKNOWN || this == REFERENCE || this == BOOL ||
			other == UNKNOWN || other == ANY || other == REFERENCE ||
			(this == INT && other == BOOL) ||
			(this == PROPLIST && other == ID);
	}
	
	public static C4Type makeType(String arg) {
		return makeType(arg, false);
	}
	
	private static final C4Type[] officialTypes = new C4Type[] {BOOL, INT, ID, STRING, ARRAY, OBJECT, PROPLIST};
	
	public static C4Type makeType(String arg, boolean allowSpecial) {
		for (C4Type t : officialTypes)
			if (t.toString().equals(arg))
				return t;
		if (allowSpecial) {
			if (arg.equals("dword")) //$NON-NLS-1$
				return C4Type.INT; // formerly DWORD
			if (arg.equals("any")) //$NON-NLS-1$
				return C4Type.ANY;
		}
		if (arg.equals("&") || (allowSpecial && arg.equals("reference"))) //$NON-NLS-1$ //$NON-NLS-2$
			return C4Type.REFERENCE;
		return C4Type.UNKNOWN;
	}
	
	
	/**
	 * Returns a type the java object comes nearest to being an instance of
	 * @param value the value
	 * @return the type
	 */
	public static C4Type typeFrom(Object value) {
		if (value instanceof String)
			return STRING;
		if (value instanceof Number)
			return INT;
		if (value instanceof Boolean)
			return BOOL;
		if (value instanceof C4ID)
			return ID;
		if (value instanceof Array)
			return ARRAY;
		if (value instanceof Map<?, ?>)
			return PROPLIST;
		return ANY;
	}
	
	/**
	 * Converts a given value to one of the calling type 
	 * @param value value to convert
	 * @return the converted value or null if conversion failed
	 */
	public Object convert(Object value) {
		C4Type valueType = typeFrom(value);
		if (valueType == this)
			return value;
		switch (this) {
		case BOOL:
			switch (valueType) {
			case INT:
				return ((Number)value).intValue() != 0;
			}
			break;
		case ANY:
			return value;
		}
		return null;
	}

}
