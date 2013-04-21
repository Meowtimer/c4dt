package net.arctics.clonk.c4script;

import java.io.Serializable;

import net.arctics.clonk.index.Definition;

/**
 * Classes implementing this interface may be a specific type or a set of types
 */
public interface IType extends Iterable<IType>, Serializable {
	
	static final String ERRONEOUS_TYPE = Messages.IType_ErroneousType;
	
	/**
	 * Return a human-readable name of this type.
	 * @param True if the resulting string should preferably be some actual C4Script-code string or not.
	 * @return The type string
	 */
	String typeName(boolean special);
	
	/**
	 * Return a static type equivalent of this type (static type of {@link Definition} is {@link PrimitiveType#OBJECT} for example)
	 * @return The static type equivalent
	 */
	IType simpleType();
	
	public static final String COMPLEX_TYPE_START = "<";
	public static final String COMPLEX_TYPE_END = ">";
	public static final String COMPLEX_TYPE_ABBREVIATED = "<...>";
}
