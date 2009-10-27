package net.arctics.clonk.parser.c4script;

import java.util.HashSet;
import java.util.Set;

import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.C4Declaration;

public class FindDeclarationInfo {
	public  ClonkIndex index;
	public int recursion;
	public Class<? extends C4Declaration> declarationClass;
	private C4Function contextFunction;
	private Set<C4ScriptBase> alreadySearched;
	private C4ScriptBase searchOrigin;

	public FindDeclarationInfo(ClonkIndex clonkIndex) {
		super();
		index = clonkIndex;
		alreadySearched = new HashSet<C4ScriptBase>();
	}
	public FindDeclarationInfo(ClonkIndex clonkIndex, C4Function ctx) {
		this(clonkIndex);
		setContextFunction(ctx);
	}
	public Class<? extends C4Declaration> getDeclarationClass() {
		return declarationClass;
	}
	public void setDeclarationClass(Class<?extends C4Declaration> declarationClass) {
		this.declarationClass = declarationClass;
	}
	public void setContextFunction(C4Function ctx) {
		contextFunction = ctx;
	}
	public C4Function getContextFunction() {
		return contextFunction;
	}
	public Set<C4ScriptBase> getAlreadySearched() {
		return alreadySearched;
	}
	public C4ScriptBase getSearchOrigin() {
		return searchOrigin;
	}
	public void setSearchOrigin(C4ScriptBase searchOrigin) {
		this.searchOrigin = searchOrigin;
	}
	public void resetState() {
		alreadySearched.clear();
		recursion = 0;
	}
}