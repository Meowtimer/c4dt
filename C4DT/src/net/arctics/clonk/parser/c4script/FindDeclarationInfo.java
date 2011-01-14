package net.arctics.clonk.parser.c4script;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.Declaration;

public class FindDeclarationInfo {
	public ClonkIndex index;
	public int recursion;
	public Class<? extends Declaration> declarationClass;
	private Function contextFunction;
	private Set<ScriptBase> alreadySearched;
	private ScriptBase searchOrigin;
	private List<ClonkIndex> relevantIndexes;

	public FindDeclarationInfo(ClonkIndex clonkIndex) {
		super();
		index = clonkIndex;
		alreadySearched = new HashSet<ScriptBase>();
	}
	public FindDeclarationInfo(ClonkIndex clonkIndex, Function ctx) {
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
	public Set<ScriptBase> getAlreadySearched() {
		return alreadySearched;
	}
	public ScriptBase getSearchOrigin() {
		return searchOrigin;
	}
	public void setSearchOrigin(ScriptBase searchOrigin) {
		this.searchOrigin = searchOrigin;
	}
	public void resetState() {
		alreadySearched.clear();
		recursion = 0;
	}
	public List<ClonkIndex> getAllRelevantIndexes() {
		if (relevantIndexes == null) {
			relevantIndexes = new ArrayList<ClonkIndex>(10);
			relevantIndexes.add(index);
			ClonkIndex.addIndexesFromReferencedProjects(relevantIndexes, index);
		}
		return relevantIndexes;
	}
}