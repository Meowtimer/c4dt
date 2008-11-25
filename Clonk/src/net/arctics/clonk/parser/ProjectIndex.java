package net.arctics.clonk.parser;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class ProjectIndex extends ClonkIndex {

	private transient IProject project;
	
	public ProjectIndex(IProject project) {
		this.project = project;
	}
	
	public void setProject(IProject proj) {
		project = proj;
	}
	
	public void fixReferencesAfterSerialization() throws CoreException {
		for (List<C4Object> list : getIndexedObjects().values()) {
			for (C4Object obj : list) {
				if (obj instanceof C4ObjectIntern) {
					C4ObjectIntern objIntern = (C4ObjectIntern)obj;
					Path path = new Path(objIntern.relativePath);
					IPath projectPath = path.removeFirstSegments(1);
					IResource res = project.findMember(projectPath);
					if (res instanceof IContainer)
						((C4ObjectIntern)obj).setCorrespondingFolder((IContainer)res);
				}
			}
		}
		for (C4ScriptBase script : getIndexedScripts()) {
			if (script instanceof C4StandaloneScript) {
				C4StandaloneScript standalone = (C4StandaloneScript) script;
				Path path = new Path(standalone.getScriptFilePath());
				IPath projectPath = path.removeFirstSegments(1);
				IResource res = project.findMember(projectPath);
				standalone.setScriptfile(res);
			}
		}
		super.fixReferencesAfterSerialization();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
