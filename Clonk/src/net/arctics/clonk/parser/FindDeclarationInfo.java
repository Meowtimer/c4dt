package net.arctics.clonk.parser;

import java.util.HashSet;
import java.util.Set;


public class FindDeclarationInfo {
	public  ClonkIndex index;
	public int recursion;
	public Class<? extends C4Field> fieldClass;
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
	public Class<? extends C4Field> getFieldClass() {
		return fieldClass;
	}
	public void setFieldClass(Class<?extends C4Field> fieldClass) {
		this.fieldClass = fieldClass;
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