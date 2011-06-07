package net.arctics.clonk.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.c4script.IHasSubDeclarations;
import net.arctics.clonk.parser.c4script.IType;

/**
 * Some type that has includes.
 * @author madeen
 *
 */
public interface IHasIncludes extends IHasSubDeclarations, IType {
	Collection<? extends IHasIncludes> getIncludes(ClonkIndex index, boolean recursive);
	boolean includes(IHasIncludes other);
	void gatherIncludes(Set<IHasIncludes> set, ClonkIndex index, boolean recursive);
	
	static final Collection<IHasIncludes> NO_INCLUDES = new ArrayList<IHasIncludes>(0);
}
