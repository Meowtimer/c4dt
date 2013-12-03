package net.arctics.clonk.c4script;

import static java.lang.String.format;

import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.Scenario;

/**
 * Helper object containing context information when doing a search for a declaration.
 * @author madeen
 *
 */
public final class FindDeclarationInfo {
	public String name;
	public Index index;
	public int recursion;
	public Class<? extends Declaration> declarationClass;
	public Function contextFunction;
	public boolean findGlobalVariables = true;
	
	private Script searchOrigin;
	private Scenario scenario;
	
	public Script searchOrigin() { return searchOrigin; }
	public Scenario scenario() { return scenario; }
	
	public void searchOrigin(final Script searchOrigin) {
		this.searchOrigin = searchOrigin;
		this.scenario = searchOrigin != null ? searchOrigin.scenario() : null;
	}
	
	private Set<Script> alreadySearched;
	private Script first;
	
	@Override
	public String toString() {
		if (declarationClass != null)
			return format("%s: %s", name, declarationClass.getSimpleName());
		return name;
	}
	
	public boolean startSearchingIn(final Script script) {
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
	public FindDeclarationInfo(final String name, final Index index) {
		super();
		this.name = name; 
		this.index = index;
	}
	/**
	 * Create an instance with a context index and function.
	 * @param index
	 * @param ctx
	 */
	public FindDeclarationInfo(final String name, final Index index, final Function ctx) {
		this(name, index);
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