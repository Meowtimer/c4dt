package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.parser.C4Declaration;

public interface IHasSubDeclarations {
	static final int VARIABLES = 1;
	static final int FUNCTIONS = 2;
	static final int INCLUDES  = 4;
	static final int OTHER = 8;
	static final int ALL_SUBDECLARATIONS = VARIABLES|FUNCTIONS|INCLUDES|OTHER;
	
	public Iterable<? extends C4Declaration> allSubDeclarations(int mask);
	String getName();
}
