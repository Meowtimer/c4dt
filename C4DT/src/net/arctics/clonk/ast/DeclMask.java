package net.arctics.clonk.ast;

import net.arctics.clonk.c4script.Directive;
import net.arctics.clonk.c4script.Variable;

/** Contains constants to combine as mask for {@link Declaration#subDeclarations(Index, int)} */
public class DeclMask {
	/**
	 * Return accessible {@link Variable}s
	 */
	public static final int VARIABLES = 1;
	/**
	 * Return accessible {@link Function}s
	 */
	public static final int FUNCTIONS = 2;
	/**
	 * Return {@link Directive}s
	 */
	public static final int DIRECTIVES = 8;
	/**
	 * Return anything else that might come up
	 */
	public static final int IMPLICIT = 16;
	/**
	 * Return static {@link Variable}s
	 */
	public static final int STATIC_VARIABLES = 32;
	/**
	 * Effects detected in a script.
	 */
	public static final int EFFECTS = 64;
	/**
	 * Proplist declarations.
	 */
	public static final int PROPLISTS = 128;
	/**
	 * Mask where every flag is set.
	 */
	public static final int ALL = Integer.MAX_VALUE;
}