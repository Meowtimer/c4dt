package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.parser.c4script.ast.ExprElm;

public interface IProplistDeclaration {

	public static final String PROTOTYPE_KEY = "Prototype";

	/**
	 * Return the implicitly set prototype expression for this declaration. Acts as fallback if no explicit 'Prototype' field is found.
	 * @return The implicit prototype
	 */
	ExprElm implicitPrototype();

	/**
	 * Whether the declaration was "explicit" {blub=<blub>...} or
	 * by assigning values separately (effect.var1 = ...; ...)
	 * @return adhoc-ness
	 */
	boolean isAdHoc();

	/**
	 * Each assignment in a proplist declaration is represented by a {@link Variable} object.
	 * @param includeAdhocComponents Whether the returned list also contains adhoc-components in addition to the ones present in the initial declaration
	 * @return Return the list of component variables this proplist declaration is made up of.
	 */
	Iterable<Variable> components(boolean includeAdhocComponents);

	/**
	 * Add a new component variable to this declaration.
	 * @param variable The variable to add
	 * @param adhoc Whether the variable is to be marked as having been added after the initial proplist parsing took place.
	 * @return Return either the passed variable or an already existing one with that name
	 */
	Variable addComponent(Variable variable, boolean adhoc);

	/**
	 * Find a component variable by name.
	 * @param declarationName The name of the variable
	 * @return The found variable or null.
	 */
	Variable findComponent(String declarationName);

	/**
	 * Return the prototype of this proplist declaration. Obtained from the special 'Prototype' entry.
	 * @return The Prototype {@link ProplistDeclaration} or null, if either the 'Prototype' entry does not exist or the type of the Prototype expression does not denote a proplist declaration.
	 */
	IProplistDeclaration prototype();

}