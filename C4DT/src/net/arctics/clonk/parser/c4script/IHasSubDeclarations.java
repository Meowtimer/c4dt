package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.parser.C4Declaration;

public interface IHasSubDeclarations {
	public Iterable<? extends C4Declaration> allSubDeclarations();
}
