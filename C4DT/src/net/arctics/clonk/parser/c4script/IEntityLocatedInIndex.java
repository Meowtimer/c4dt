package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.Index;

/**
 * Some named or describable entity contained in a {@link Index}
 * @author madeen
 *
 */
public interface IEntityLocatedInIndex {
	/**
	 * Get the name. This may also be some vague description-
	 * @return The name or description
	 */
	String name();
	/**
	 * Return the index the entity is contained in.
	 * @return The {@link Index}
	 */
	Index getIndex();
}
