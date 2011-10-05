package net.arctics.clonk.parser.c4script;

import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;

public class FindDeclarationInfo {
	public Index index;
	public int recursion;
	public Class<? extends Declaration> declarationClass;
	private Function contextFunction;
	private Set<Script> alreadySearched;
	private Script searchOrigin;
	
	public FindDeclarationInfo(Index clonkIndex) {
		super();
		index = clonkIndex;
		alreadySearched = new HashSet<Script>();
	}
	public FindDeclarationInfo(Index clonkIndex, Function ctx) {
		this(clonkIndex);
		setContextFunction(ctx);
	}
	public Class<? extends Declaration> getDeclarationClass() {
		return declarationClass;
	}
	public void setDeclarationClass(Class<?extends Declaration> declarationClass) {
		this.declarationClass = declarationClass;
	}
	public void setContextFunction(Function ctx) {
		contextFunction = ctx;
	}
	public Function getContextFunction() {
		return contextFunction;
	}
	public Set<Script> getAlreadySearched() {
		return alreadySearched;
	}
	public Script getSearchOrigin() {
		return searchOrigin;
	}
	public void setSearchOrigin(Script searchOrigin) {
		this.searchOrigin = searchOrigin;
	}
	public void resetState() {
		alreadySearched.clear();
		recursion = 0;
	}
}