package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.parser.c4script.ast.TypeExpectancyMode;
import net.arctics.clonk.parser.c4script.ast.TypeUnification;

/**
 * Entity that can be assigned a type.
 * @author madeen
 *
 */
public interface ITypeable extends IIndexEntity {
	/**
	 * The outside world expects this entity to be of the specified type. Act accordingly.
	 * @param t The type this entity is expected to be of
	 * @param mode Strength of expectancy expressed in {@link TypeExpectancyMode} values
	 */
	public void expectedToBeOfType(IType t, TypeExpectancyMode mode);
	/**
	 * Return the current type the entity is deemed to be of.
	 * @return The current type
	 */
	public IType type();
	/**
	 * Force the type of this entity.
	 * @param type The type to force the entity to be of
	 */
	public void forceType(IType type);
	/**
	 * Assign a type. Calling this method will not have an effect if {@link #staticallyTyped()} is already set to true.
	 * @param type The type to assign
	 * @param _static Whether to assign the type and seal the type of this {@link ITypeable} as {@link #staticallyTyped()} afterwards, preventing further reassignments
	 */
	public void assignType(IType type, boolean _static);
	/**
	 * Returns true if the type of this entity cannot be changed anymore. 
	 * @return True for static typedness, false otherwise
	 */
	boolean staticallyTyped();
	
	/**
	 * Whether this typeable can only be accessed from some restricted area (i.e. a parameter or local variable)
	 * @return
	 */
	boolean isLocal();
	
	/**
	 * Some predefined default implementations for implementing classes.
	 * @author madeen
	 *
	 */
	public abstract static class Default {
		/**
		 * Default implementation for {@link #expectedToBeOfType(ITypeable, IType)}.
		 * Will call {@link #forceType(IType)} on the instance with either the passed type or a {@link TypeSet} comprised of the current instance type and the specified one. 
		 * @param instance The {@link ITypeable} instance to perform default operations for
		 * @param type The type as passed by a call to {@link #expectedToBeOfType(ITypeable, IType)}
		 */
		public static void expectedToBeOfType(ITypeable instance, IType type) {
			if (instance.type() == PrimitiveType.UNKNOWN)
				// unknown before so now it is assumed to be of this type
				instance.assignType(type, false);
			else if (!instance.type().equals(type))
				// assignments of multiple types - declaration now has multiple potential types
				instance.assignType(TypeUnification.unify(type, instance.type()), false);
		}
	}
}
