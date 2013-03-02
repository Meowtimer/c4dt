package net.arctics.clonk.index;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Directive;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IHasName;
import net.arctics.clonk.parser.c4script.Variable;

/**
 * Declaration that has sub declarations.
 * @author madeen
 *
 */
public interface IHasSubDeclarations extends IHasName {
	/** Contains constants to combine as mask for {@link #subDeclarations(Index, int)} */
	public static class DeclMask {
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
	/**
	 * Return an {@link Iterable} to iterate over declarations accessible from this object that match the supplied bit mask
	 * @param contextIndex {@link Index} Context index. Required for correctly returning appended scripts if a project a completion proposal is invoked in contains scripts appending themselves to scripts from another project.
	 * @param mask a bit mask specifying what to include in the returned {@link Iterable}, formed by the static variables in this interface
	 * @return An iterable to iterate over sub declarations satifying the passed mask
	 */
	Iterable<? extends Declaration> subDeclarations(Index contextIndex, int mask);
}
