package net.arctics.clonk.parser.c4script;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;

import net.arctics.clonk.parser.C4ID;

/**
 * The engine predefined variable types.
 * @author ZokRadonh
 *
 */
public enum C4Type implements ITypeSet {
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
	
	private String lowercaseName;
	
	@Override
	public String toString() {
		return toString(false);
	}
	
	@Override
	public String toString(boolean special) {
		if (!special && this == REFERENCE)
			return "&"; //$NON-NLS-1$
		if (lowercaseName == null)
			lowercaseName = super.toString().toLowerCase();
		return lowercaseName;
	}
	
	@Override
	public boolean canBeAssignedFrom(ITypeSet other) {
		for (C4Type t : other) {
			if (t == this)
				return true;
			switch (t) {
			case UNKNOWN: case ANY: case REFERENCE:
				return true;
			default:
				switch (this) {
				case ANY: case UNKNOWN: case REFERENCE: case BOOL:
					return true;
				case INT:
					return other == BOOL;
				case PROPLIST:
					return other == ID || other == OBJECT;
				}
			}
		}
		return false;
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

	/**
	 * Awesomely return iterator that iterates over this type
	 */
	@Override
	public Iterator<C4Type> iterator() {
		return new Iterator<C4Type>() {
			private boolean done = false;
			
			@Override
			public boolean hasNext() {
				return !done;
			}

			@Override
			public C4Type next() {
				done = true;
				return C4Type.this;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public boolean subsetOf(ITypeSet typeSet) {
		for (C4Type t : typeSet)
			if (t == this)
				return true;
		return false;
	}

	@Override
	public boolean contains(C4Type type) {
		return type == this;
	}
	
	@Override
	public int specificness() {
		switch (this) {
		case UNKNOWN:
			return 0;
		case ANY:
			return 1;
		case REFERENCE:
			return 2;
		default:
			return 3;
		}
	}

}
