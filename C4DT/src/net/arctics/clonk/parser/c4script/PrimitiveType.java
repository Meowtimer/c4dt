package net.arctics.clonk.parser.c4script;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.parser.ID;
import net.arctics.clonk.util.ArrayUtil;

/**
 * The engine predefined variable types.
 * @author ZokRadonh
 *
 */
public enum PrimitiveType implements IType {
	UNKNOWN,
	
	ANY,
	BOOL,
	INT,
	ID,
	STRING,
	ARRAY,
	OBJECT, 
	REFERENCE,
	PROPLIST,
	FUNCTION;
	
	private String lowercaseName;
	
	@Override
	public String toString() {
		return typeName(false);
	}
	
	@Override
	public String typeName(boolean special) {
		if (!special && this == REFERENCE)
			return "&"; //$NON-NLS-1$
		if (lowercaseName == null) {
			lowercaseName = super.toString().toLowerCase();
		}
		return lowercaseName;
	}

	/**
	 * Map to map type names from Clonk engine source to primitive types.
	 */
	public static final Map<String, PrimitiveType> CPP_TO_C4SCRIPT_MAP = ArrayUtil.map(
		false,
		"C4Value", PrimitiveType.ANY,
		"C4Void", PrimitiveType.ANY,
		"long", PrimitiveType.INT,
		"int", PrimitiveType.INT,
		"bool", PrimitiveType.BOOL,
		"C4ID", PrimitiveType.ID,
		"C4Object", PrimitiveType.OBJECT,
		"C4PropList", PrimitiveType.PROPLIST,
		"C4Value", PrimitiveType.ANY,
		"C4String", PrimitiveType.STRING,
		"C4Void", PrimitiveType.UNKNOWN
	);
	
	/**
	 * Map to map primitive types to type names from Clonk engine source.
	 */
	public static final Map<PrimitiveType, String> C4SCRIPT_TO_CPP_MAP = ArrayUtil.reverseMap(CPP_TO_C4SCRIPT_MAP, new HashMap<PrimitiveType, String>());
	
	private static final Pattern nillablePattern = Pattern.compile("Nillable\\<(.*?)\\>");
	private static final Pattern pointerTypePattern = Pattern.compile("(.*?)\\s*?\\*");
	
	/**
	 * Return a primitive type from a C++ type string
	 * @param type The C++ type string to interpret
	 * @return The primitive type or {@link #UNKNOWN} if no 
	 */
	public static PrimitiveType typeFromCPPType(String type) {
		Matcher m;
		PrimitiveType ty = PrimitiveType.CPP_TO_C4SCRIPT_MAP.get(type);
		if (ty != null) {
			return ty;
		}
		if ((m = nillablePattern.matcher(type)).matches()) {
			return typeFromCPPType(m.group(1));
		} else if ((m = pointerTypePattern.matcher(type)).matches()) {
			String t = m.group(1);
			ty = typeFromCPPType(t);
			if (ty != null) {
				return ty;
			}
		}
		return PrimitiveType.UNKNOWN; 
	}
	
	public static String cppTypeFromType(IType type) {
		PrimitiveType t = makeType(type.toString());
		return C4SCRIPT_TO_CPP_MAP.get(t);
	}
	
	@Override
	public boolean canBeAssignedFrom(IType other) {
		if (other != null) for (IType t : other) {
			t = t.staticType();
			if (t == this)
				return true;
			if (t.getClass() == PrimitiveType.class) {
				switch ((PrimitiveType)t) {
				case UNKNOWN: case ANY: case REFERENCE:
					return true;
				default:
					switch (this) {
					case ANY: case UNKNOWN: case REFERENCE: case BOOL:
						return true;
					case INT:
						if (other == BOOL)
							return true;
					case PROPLIST:
						if (t == ID || t == OBJECT)
							return true;
					case OBJECT:
						if (t == PROPLIST)
							return true;
					}
				}
			}
		}
		return false;
	}

	public static PrimitiveType makeType(String arg) {
		return makeType(arg, false);
	}
	
	public static PrimitiveType makeType(String arg, boolean allowSpecial) {
		for (PrimitiveType t : values())
			if (t.toString().equals(arg))
				return t;
		if (allowSpecial) {
			if (arg.equals("dword")) //$NON-NLS-1$
				return PrimitiveType.INT; // formerly DWORD
			if (arg.equals("any")) //$NON-NLS-1$
				return PrimitiveType.ANY;
		}
		if (arg.equals("&") || (allowSpecial && arg.equals("reference"))) //$NON-NLS-1$ //$NON-NLS-2$
			return PrimitiveType.REFERENCE;
		return PrimitiveType.UNKNOWN;
	}

	/**
	 * Returns a type the java object comes nearest to being an instance of
	 * @param value the value
	 * @return the type
	 */
	public static PrimitiveType typeFrom(Object value) {
		if (value instanceof String)
			return STRING;
		if (value instanceof Number)
			return INT;
		if (value instanceof Boolean)
			return BOOL;
		if (value instanceof ID)
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
		PrimitiveType valueType = typeFrom(value);
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
	public Iterator<IType> iterator() {
		return new Iterator<IType>() {
			private boolean done = false;
			
			@Override
			public boolean hasNext() {
				return !done;
			}

			@Override
			public PrimitiveType next() {
				done = true;
				return PrimitiveType.this;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public boolean intersects(IType typeSet) {
		for (IType t : typeSet) {
			for (IType t2 : this) {
				if (t.canBeAssignedFrom(t2) || t2.canBeAssignedFrom(t))
					return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsType(IType type) {
		IType staticType = type.staticType();
		if (staticType == this) {
			return true;
		}
		if (staticType instanceof PrimitiveType) {
			switch ((PrimitiveType)staticType) {
			case ANY:
			case UNKNOWN:
			case ARRAY:
			case ID:
			case OBJECT:
			case INT:
			case STRING:
				return this == ANY || this == REFERENCE;
			case PROPLIST:
				return this == OBJECT || this == ID;
			case BOOL:
				return this == INT;
			case REFERENCE:
				return this == ANY || this == REFERENCE;
			}
		}
		return false;
	}
	
	@Override
	public boolean containsAnyTypeOf(IType... types) {
		for (IType t : types)
			if (containsType(t))
				return true;
		return false;
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

	@Override
	public IType staticType() {
		return this;
	}
	
	@Override
	public void setTypeDescription(String description) {}

}
