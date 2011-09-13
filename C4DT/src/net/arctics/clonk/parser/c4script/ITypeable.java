package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.parser.c4script.ast.TypeExpectancyMode;

/**
 * Entity that can be assigned a type.
 * @author madeen
 *
 */
public interface ITypeable extends IEntityLocatedInIndex {
	/**
	 * The outside world expects this entity to be of the specified type. Act accordingly.
	 * @param t The type this entity is expected to be of
	 * @param mode Strength of expectency expressed in {@link TypeExpectancyMode} values
	 */
	public void expectedToBeOfType(IType t, TypeExpectancyMode mode);
	/**
	 * Return the current type the entity is deemed to be of.
	 * @return The current type
	 */
	public IType getType();
	/**
	 * Force the type of this entity.
	 * @param type The type to force the entity to be of
	 */
	public void forceType(IType type);
	/**
	 * Returns true if the type of this entity cannot be changed anymore. 
	 * @return True for type invariance, false otherwise
	 */
	boolean typeIsInvariant();
	
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
			if (instance.getType() == PrimitiveType.UNKNOWN)
				// unknown before so now it is assumed to be of this type
				instance.forceType(type);
			else if (instance.getType().equals(type))
				// assignments of multiple types - declaration now has multiple potential types
				instance.forceType(TypeSet.create(type, instance.getType()));
		}
	}
}
