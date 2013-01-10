package net.arctics.clonk.parser.c4script;

import java.io.Serializable;

import net.arctics.clonk.index.Definition;

/**
 * Classes implementing this interface may be a specific type or a set of types
 */
public interface IType extends Iterable<IType>, Serializable {
	
	static final String ERRONEOUS_TYPE = Messages.IType_ErroneousType;

	/**
	 * Return whether some variable of this type can be assigned a value of the other type.
	 * @param other The type of the value to be assigned to the variable
	 * @return True if possible, false if not.
	 */
	boolean canBeAssignedFrom(IType other);
	
	/**
	 * Return a human-readable name of this type.
	 * @param True if the resulting string should preferably be some actual C4Script-code string or not.
	 * @return The type string
	 */
	String typeName(boolean special);
		
	/**
	 * Return an integer signifying the level of precision. Actual C4Script definitions are supposed to be more specific than {@link PrimitiveType#OBJECT} for example.
	 * @return The precision of the type as integer
	 */
	int precision();
	
	/**
	 * Return a static type equivalent of this type (static type of {@link Definition} is {@link PrimitiveType#OBJECT} for example)
	 * @return The static type equivalent
	 */
	IType simpleType();
	/**
	 * Description of the type. Will be incorporated into {@link #typeName(boolean)} if set. There is no guarantee that calling this method will actually result in some description being set.
	 * @param description The description explaining how this type was constructed
	 */
	void setTypeDescription(String description);
	
	public static final String COMPLEX_TYPE_START = "<";
	public static final String COMPLEX_TYPE_END = ">";
	public static final String COMPLEX_TYPE_ABBREVIATED = "<...>";
}
