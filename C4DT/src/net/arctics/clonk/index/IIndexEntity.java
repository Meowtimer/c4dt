package net.arctics.clonk.index;

import java.util.regex.Matcher;


/**
 * Some named or describable entity contained in a {@link Index}
 * @author madeen
 *
 */
public interface IIndexEntity {
	/**
	 * Get the name. This may also be some vague description-
	 * @return The name or description
	 */
	String name();
	/**
	 * Return whether this entity has some relevant textual description that is matched by the given matcher.
	 * Used for filtering dialogs which let the user type arbitrary text to locate what she is looking for. 
	 * @param matcher The matcher
	 * @return true if matched, false if not.
	 */
	boolean matchedBy(Matcher matcher);
	/**
	 * Return some informational text describing this entity for the user.
	 * @return Informational text
	 */
	String infoText(IIndexEntity context);
	/**
	 * Return the index the entity is contained in.
	 * @return The {@link Index}
	 */
	Index index();
}
