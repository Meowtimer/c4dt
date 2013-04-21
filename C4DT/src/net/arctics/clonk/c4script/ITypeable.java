package net.arctics.clonk.c4script;

import net.arctics.clonk.index.IIndexEntity;

/**
 * Entity that can be assigned a type.
 * @author madeen
 *
 */
public interface ITypeable extends IIndexEntity {
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
}
