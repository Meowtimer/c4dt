package net.arctics.clonk.parser;

import net.arctics.clonk.parser.c4script.Directive;

/** Contains constants to combine as mask for {@link Declaration#subDeclarations(Index, int)} */
public class DeclMask {
	/**
	 * Return accessible {@link Variable}s
	 */
	public static final int VARIABLES = 1;
	/**
	 * Return static {@link Variable}s
	 */
	public static final int STATIC_VARIABLES = 32;
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
	 * Mask including {@link VARIABLES}, {@link FUNCTIONS}, {@link DIRECTIVES} and {@link IMPLICIT}
	 */
	public static final int ALL = VARIABLES|FUNCTIONS|DIRECTIVES|IMPLICIT;
}