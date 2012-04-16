package net.arctics.clonk.parser.c4script;

import java.util.List;

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
	 * @return Return the list of component variables this proplist declaration is made up of.
	 */
	List<Variable> components();

	/**
	 * Add a new component variable to this declaration.
	 * @param variable The variable to add
	 * @return Return either the passed variable or an already existing one with that name
	 */
	Variable addComponent(Variable variable);

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