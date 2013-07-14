package net.arctics.clonk.ast;


/**
 * Implemented by {@link ASTNode}s which represent a distinct section in a larger tree.
 * A function would be an example.
 * @author madeen
 *
 */
public interface IASTSection {
	/**
	 * Return the absolute text offset of this section.
	 * @return The offset
	 */
	int absoluteOffset();
}
