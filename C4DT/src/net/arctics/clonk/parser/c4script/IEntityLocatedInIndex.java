package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.ClonkIndex;

/**
 * Some named or describable entity contained in a {@link ClonkIndex}
 * @author madeen
 *
 */
public interface IEntityLocatedInIndex {
	/**
	 * Get the name. This may also be some vague description-
	 * @return The name or description
	 */
	String getName();
	/**
	 * Return the index the entity is contained in.
	 * @return The {@link ClonkIndex}
	 */
	ClonkIndex getIndex();
}
