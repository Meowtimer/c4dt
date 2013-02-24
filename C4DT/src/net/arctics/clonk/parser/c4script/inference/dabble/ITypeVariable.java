package net.arctics.clonk.parser.c4script.inference.dabble;

import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ITypeable;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.inference.dabble.DabbleInference.ScriptProcessor;

/**
 * Type information stored for some expression.
 * @author madeen
 *
 */
public interface ITypeVariable {
	/**
	 * Type stored for the expression.
	 * @return The type.
	 */
	IType get();
	/**
	 * Store the type.
	 * @param type The type to store
	 */
	void set(IType type);
	/**
	 * Hint that the expression this {@link ITypeVariable} was created for might be of the given type.
	 * @param type The type to hint at
	 * @return Return true if hinting resulted in changing the type or if the already-set type intersects with the new one.
	 */
	boolean hint(IType type);
	/**
	 * Return whether the stored type information stores type information for the given expression so that creating a new stored type information object is not necessary.
	 * @param expr The expression to test for
	 * @param processor Processor serving as context
	 * @return True, if relevant, false if not.
	 */
	boolean binds(ASTNode expr, ScriptProcessor processor);
	/**
	 * Return whether another {@link ITypeVariable} refers to the same expression as this one.
	 * @param other The other stored type information
	 * @return True, if same expression, false if not.
	 */
	boolean same(ITypeVariable other);
	/**
	 * Apply this stored type information so the underlying {@link ITypeable} ({@link Variable}, {@link Function} etc) will have its type set.
	 * @param soft Apply 'softly', meaning that permanent type changes won't be applied.
	 * @param processor Processor serving as context
	 */
	void apply(boolean soft, ScriptProcessor processor);
	/**
	 * Merge type information with another one which is refering to the same expression.
	 * @param other The other type information
	 */
	void merge(ITypeVariable other);
	/**
	 * Declare here since Cloneable clone is seemingly magic.
	 * @return The clone
	 * @throws CloneNotSupportedException
	 */
	Object clone() throws CloneNotSupportedException; // Cloneable does not declare the method :c
	Declaration declaration(ScriptProcessor processor);
}
