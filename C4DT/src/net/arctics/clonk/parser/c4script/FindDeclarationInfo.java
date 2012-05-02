package net.arctics.clonk.parser.c4script;

import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;

/**
 * Helper object containing context information when doing a search for a declaration.
 * @author madeen
 *
 */
public class FindDeclarationInfo {
	public Index index;
	public int recursion;
	public Class<? extends Declaration> declarationClass;
	public Function contextFunction;
	public Set<Script> alreadySearched;
	public Script searchOrigin;
	public boolean findGlobalVariables = true;
	
	/**
	 * Create an instance with a context index.
	 */
	public FindDeclarationInfo(Index clonkIndex) {
		super();
		index = clonkIndex;
		alreadySearched = new HashSet<Script>();
	}
	/**
	 * Create an instance with a context index and function.
	 * @param clonkIndex
	 * @param ctx
	 */
	public FindDeclarationInfo(Index clonkIndex, Function ctx) {
		this(clonkIndex);
		contextFunction = ctx;
	}
	/**
	 * Reset state information, namely {@link #alreadySearched} and {@link #recursion}
	 */
	public void resetState() {
		alreadySearched.clear();
		recursion = 0;
	}
}