package net.arctics.clonk.c4script;

import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.index.Index;

/**
 * Helper object containing context information when doing a search for a declaration.
 * @author madeen
 *
 */
public final class FindDeclarationInfo {
	public Index index;
	public int recursion;
	public Class<? extends Declaration> declarationClass;
	public Function contextFunction;
	public Script searchOrigin;
	public boolean findGlobalVariables = true;
	
	private Set<Script> alreadySearched;
	private Script first;
	
	public boolean startSearchingIn(Script script) {
		if (script == first)
			return false;
		else if (first == null)
			first = script;
		else {
			if (alreadySearched == null)
				alreadySearched = new HashSet<>(3);
			if (!alreadySearched.add(script))
				return false;
		}
		return true;
	}
	
	/**
	 * Create an instance with a context index.
	 */
	public FindDeclarationInfo(Index clonkIndex) {
		super();
		index = clonkIndex;
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
		first = null;
		alreadySearched = null;
		recursion = 0;
	}
}