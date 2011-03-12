package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.parser.Declaration;

/**
 * Interface to be implemented by declarations that can return a list of sub declarations (including #included scripts) 
 * @author madeen
 *
 */
public interface IHasSubDeclarations {
	static final int VARIABLES = 1;
	static final int FUNCTIONS = 2;
	static final int INCLUDES  = 4;
	static final int DIRECTIVES = 8;
	static final int OTHER = 16;
	static final int DIRECT_SUBDECLARATIONS = VARIABLES|FUNCTIONS|DIRECTIVES|OTHER;
	
	public Iterable<? extends Declaration> allSubDeclarations(int mask);
	String getName();
}
