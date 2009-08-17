package net.arctics.clonk.index;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptIntern;
import net.arctics.clonk.resource.ExternalLib;
import net.arctics.clonk.util.IPredicate;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class ProjectIndex extends ClonkIndex {

	private static final long serialVersionUID = 1L;
	
	private transient IProject project;
	private Collection<String> dependencyNames;
	private BitSet externalLibBitSet;
	
	public List<ExternalLib> getDependencies() {
		List<ExternalLib> allLibs = ClonkCore.getDefault().getExternIndex().getLibs();
		// no explicit dependencies specified; return all set in preferences
		if (dependencyNames == null)
			return allLibs;
		// filter
		return Utilities.filter(allLibs, new IPredicate<ExternalLib>() {
			public boolean test(ExternalLib lib) {
				return Utilities.collectionContains(dependencyNames, lib.getNodeName());
			}
		});
	}
	
	public void setDependencies(List<ExternalLib> list) {
		Collection<String> depNames = new ArrayList<String>(list.size());
		for (ExternalLib lib : list)
			depNames.add(lib.getNodeName());
		this.dependencyNames = depNames;
		notifyExternalLibsSet();
	}
	
	public void setDependencies(Collection<String> list) {		
		this.dependencyNames = list;
		notifyExternalLibsSet();
	}
	
	@Override
	public boolean acceptsFromExternalLib(ExternalLib lib) {
		return externalLibBitSet == null || externalLibBitSet.get(lib.getIndex());
	}
	
	public void notifyExternalLibsSet() {
		if (dependencyNames == null)
			externalLibBitSet = null;
		else {
			List<ExternalLib> allLibs = ClonkCore.getDefault().getExternIndex().getLibs();
			externalLibBitSet = new BitSet(allLibs.size());
			for (ExternalLib lib : allLibs) {
				externalLibBitSet.set(lib.getIndex(), Utilities.collectionContains(dependencyNames, lib.getNodeName()));
			}
		}
	}
	
	public ProjectIndex(IProject project) {
		this.project = project;
	}
	
	public void setProject(IProject proj) {
		project = proj;
	}
	
	public void fixReferencesAfterSerialization() throws CoreException {
		for (C4Object object : this) {
			if (object instanceof C4ObjectIntern) {
				((C4ObjectIntern)object).refreshFolderReference(project);
			}
		}
		for (C4Scenario scenario : getIndexedScenarios()) {
			scenario.refreshFolderReference(project);
		}
		for (C4ScriptBase script : getIndexedScripts()) {
			if (script instanceof C4ScriptIntern) {
				C4ScriptIntern standalone = (C4ScriptIntern) script;
				Path path = new Path(standalone.getScriptFilePath());
				IPath projectPath = path.removeFirstSegments(1);
				IResource res = project.findMember(projectPath);
				standalone.setScriptFile(res);
			}
		}
		super.fixReferencesAfterSerialization();
	}
	
	@Override
	public String toString() {
		return "Index for " + project.toString();
	}

	public Iterable<String> getDependencyNames() {
		return dependencyNames;
	}

}
