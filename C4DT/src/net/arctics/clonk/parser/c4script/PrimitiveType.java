package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.ArrayUtil.map;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.ISerializationResolvable;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.util.ArrayUtil;

import org.omg.CORBA.UNKNOWN;

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
	FUNCTION,
	FLOAT,
	NUM,
	ERRONEOUS;

	public static final PrimitiveType[] NILLABLES = {
		OBJECT,
		STRING,
		ARRAY,
		PROPLIST,
		ID,
		FUNCTION
	};
	private final ReferenceType referenceType = new ReferenceType(this);

	public IType referenceType() { return referenceType; }

	private String scriptName;
	public String scriptName() {return scriptName;}

	@Override
	public String toString() { return typeName(true); }

	private static final Map<String, PrimitiveType> REGULAR_MAP = new HashMap<String, PrimitiveType>();
	private static final Map<String, PrimitiveType> SPECIAL_MAPPING = map(false,
		"dword", INT,
		"any", ANY,
		"reference", REFERENCE,
		"void", UNKNOWN
	);
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
		"C4Def*", PrimitiveType.ID,
		"C4Object*", PrimitiveType.OBJECT,
		"C4PropList*", PrimitiveType.PROPLIST,
		"C4Value", PrimitiveType.ANY,
		"C4String*", PrimitiveType.STRING,
		"C4Void*", PrimitiveType.UNKNOWN,
		"C4AulFunc*", PrimitiveType.FUNCTION
	);
	/**
	 * Map to map primitive types to type names from Clonk engine source.
	 */
	public static final Map<PrimitiveType, String> C4SCRIPT_TO_CPP_MAP = ArrayUtil.reverseMap(CPP_TO_C4SCRIPT_MAP, new HashMap<PrimitiveType, String>());

	static {
		for (final PrimitiveType t : values()) {
			switch (t) {
			case REFERENCE:
				t.scriptName = "&";
				break;
			case ERRONEOUS:
				t.scriptName = "<error>";
				continue;
			default:
				t.scriptName = t.name().toLowerCase();
			}
			REGULAR_MAP.put(t.scriptName, t);
		}
	}

	@Override
	public String typeName(boolean special) {
		if (!special && this == UNKNOWN)
			return ANY.typeName(false);
		else
			return scriptName;
	}

	private static final Pattern NILLABLE_PATTERN = Pattern.compile("Nillable\\<(.*?)\\>");
	private static final Pattern POINTERTYPE_PATTERN = Pattern.compile("(.*?)\\s*?\\*");

	/**
	 * Return a primitive type from a C++ type string
	 * @param type The C++ type string to interpret
	 * @return The primitive type or {@link #UNKNOWN} if no
	 */
	public static PrimitiveType fromCPPString(String type) {
		Matcher m;
		PrimitiveType ty = PrimitiveType.CPP_TO_C4SCRIPT_MAP.get(type);
		if (ty != null)
			return ty;
		if ((m = NILLABLE_PATTERN.matcher(type)).matches())
			return fromCPPString(m.group(1));
		else if ((m = POINTERTYPE_PATTERN.matcher(type)).matches()) {
			final String t = m.group(1);
			ty = fromCPPString(t);
			if (ty != null)
				return ty;
		}
		return PrimitiveType.UNKNOWN;
	}

	public static String CPPTypeFromType(IType type) {
		final PrimitiveType t = fromString(type.toString());
		return C4SCRIPT_TO_CPP_MAP.get(t);
	}

	/**
	 * Return a {@link PrimitiveType} parsed from a C4Script type string. If the string does not specify a type, {@link UNKNOWN} is returned.
	 * @param arg The C4Script type string to return a primitive type for
	 * @return The primitive type or {@link UNKNOWN}.
	 */
	public static PrimitiveType fromString(String arg) {
		return defaulting(fromString(arg, false), UNKNOWN);
	}

	/**
	 * Return {@link PrimitiveType} parsed from a type string that can be a regular C4Script type string or
	 * if allowSpecial is passed true some 'special' type string which would not be allowed by the engine when parsing a script.
	 * @param typeString The type string
	 * @param allowSpecial Whether to allow special syntax
	 * @return The {@link PrimitiveType} parsed from the argument or null if not successful.
	 */
	public static PrimitiveType fromString(String typeString, boolean allowSpecial) {
		final PrimitiveType t = REGULAR_MAP.get(typeString);
		if (t != null)
			return t;
		if (allowSpecial)
			return SPECIAL_MAPPING.get(typeString);
		return null;
	}

	/**
	 * Returns a type the java object comes nearest to being an instance of
	 * @param value the value
	 * @return the type
	 */
	public static PrimitiveType correspondingToInstance(Object value) {
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
		final PrimitiveType valueType = correspondingToInstance(value);
		if (valueType == this)
			return value;
		switch (this) {
		case BOOL:
			switch (valueType) {
			case INT:
				return ((Number)value).intValue() != 0;
			default:
				break;
			}
			break;
		case ANY:
			return value;
		default:
			break;
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
	public IType simpleType() {
		return this;
	}

	public class Unified implements IType, ISerializationResolvable {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		@Override
		public Iterator<IType> iterator() { return PrimitiveType.this.iterator(); }
		@Override
		public String typeName(boolean special) { return PrimitiveType.this.typeName(special); }
		@Override
		public String toString() { return typeName(true); }
		@Override
		public IType simpleType() { return PrimitiveType.this; }
		@Override
		public Object resolve(Index index) { return PrimitiveType.this.unified(); }
		public PrimitiveType base() { return PrimitiveType.this; }
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PrimitiveType)
				return obj == PrimitiveType.this;
			else if (obj instanceof Unified)
				return ((Unified)obj).simpleType() == this.simpleType();
			else
				return false;
		}
	}

	final Unified unified = new Unified();

	/**
	 * Return a type signifying the result of unification which ended with this primitive type.
	 * Further unification involving this type will not result in the unification result getting more specialized again.
	 * @return The unified version of this primitive type
	 */
	public final Unified unified() { return unified; }
}
