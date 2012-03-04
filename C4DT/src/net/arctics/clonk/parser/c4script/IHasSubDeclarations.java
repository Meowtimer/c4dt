package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;

/**
 * Declaration that has sub declarations. 
 * @author madeen
 *
 */
public interface IHasSubDeclarations extends IHasName {
	
	/**
	 * Return accessible {@link Variable}s
	 */
	static final int VARIABLES = 1;
	/**
	 * Return accessible {@link Function}s
	 */
	static final int FUNCTIONS = 2;
	/**
	 * Return {@link Directive}s
	 */
	static final int DIRECTIVES = 8;
	/**
	 * Return anything else that might come up
	 */
	static final int OTHER = 16;
	/**
	 * Return static {@link Variable}s
	 */
	static final int STATIC_VARIABLES = 32;
	
	/**
	 * Mask including {@link #VARIABLES}, {@link #FUNCTIONS}, {@link #DIRECTIVES} and {@link #OTHER}
	 */
	static final int ALL = VARIABLES|FUNCTIONS|DIRECTIVES|OTHER;
	
	/**
	 * Return an {@link Iterable} to iterate over declarations accessible from this object that match the supplied bit mask
	 * @param contextIndex {@link Index} Context index. Required for correctly returning appended scripts if a project a completion proposal is invoked in contains scripts appending themselves to scripts from another project. 
	 * @param mask a bit mask specifying what to include in the returned {@link Iterable}, formed by the static variables in this interface
	 * @return
	 */
	Iterable<? extends Declaration> subDeclarations(Index contextIndex, int mask);
	Function findFunction(String functionName);
	Declaration findDeclaration(String name, FindDeclarationInfo info);
}
