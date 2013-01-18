package net.arctics.clonk.parser.c4script.ast;

import net.arctics.clonk.parser.ExprElm;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.ITypeable;
import net.arctics.clonk.parser.c4script.Variable;

/**
 * Type information stored for some expression.
 * @author madeen
 *
 */
public interface ITypeInfo {
	/**
	 * Type stored for the expression.
	 * @return The type.
	 */
	IType type();
	/**
	 * Store the type.
	 * @param type The type to store
	 */
	void storeType(IType type);
	/**
	 * Hint that the expression this {@link ITypeInfo} was created for might be of the given type.
	 * @param type The type to hint at
	 * @return Return true if hinting resulted in changing the type or if the already-set type intersects with the new one. 
	 */
	boolean hint(IType type);
	/**
	 * Return whether the stored type information stores type information for the given expression so that creating a new stored type information object is not necessary.
	 * @param expr The expression to test for
	 * @param parser Parser, acting as context
	 * @return True, if relevant, false if not.
	 */
	boolean storesTypeInformationFor(ExprElm expr, C4ScriptParser parser);
	/**
	 * Return whether another {@link ITypeInfo} refers to the same expression as this one.
	 * @param other The other stored type information
	 * @return True, if same expression, false if not.
	 */
	boolean refersToSameExpression(ITypeInfo other);
	/**
	 * Apply this stored type information so the underlying {@link ITypeable} ({@link Variable}, {@link Function} etc) will have its type set.
	 * @param soft Apply 'softly', meaning that permanent type changes won't be applied. 
	 * @param parser Parser, acting as context
	 */
	void apply(boolean soft, C4ScriptParser parser);
	/**
	 * Merge type information with another one which is refering to the same expression.
	 * @param other The other type information
	 */
	void merge(ITypeInfo other);
	/**
	 * Declare here since Cloneable clone is seemingly magic.
	 * @return The clone
	 * @throws CloneNotSupportedException
	 */
	Object clone() throws CloneNotSupportedException; // Cloneable does not declare the method :c
}
