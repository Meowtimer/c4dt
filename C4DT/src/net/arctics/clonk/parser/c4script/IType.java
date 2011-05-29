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
	 * Return whether there is an intersection between this type and the other one.
	 * @param typeSet The other type or set of types to check intersection of
	 * @return True or false
	 */
	boolean intersects(IType typeSet);
	
	/**
	 * Return whether instances of the other type are a subset of the instances of this type.
	 * @param type The type
	 * @return True or false.
	 */
	boolean containsType(IType type);
	
	/**
	 * Return whether any type in the given array of types is contained in this one.
	 * @param types The other types
	 * @return True or false.
	 */
	boolean containsAnyTypeOf(IType... types);
	
	/**
	 * Return an integer signifying the level of specificness. Actual C4Script definitions are supposed to be more specific than {@link PrimitiveType#OBJECT} for example.
	 * @return The specificness of the type as integer
	 */
	int specificness();
	
	/**
	 * Return a static type equivalent of this type (static type of {@link Definition} is {@link PrimitiveType#OBJECT} for example)
	 * @return The static type equivalent
	 */
	IType staticType();
	/**
	 * Description of the type. Will be incorporated into {@link #typeName(boolean)} if set. There is no guarantee that calling this method will actually result in some description being set.
	 * @param description The description explaining how this type was constructed
	 */
	void setTypeDescription(String description);
	
	/**
	 * Helper class defining some default implementations implementors can call.
	 * @author madeen
	 *
	 */
	public abstract class Default {
		public static boolean containsAnyTypeOf(IType instance, IType... types) {
			for (IType t : types)
				if (instance.containsType(t))
					return true;
			return false;
		}
	}
}
