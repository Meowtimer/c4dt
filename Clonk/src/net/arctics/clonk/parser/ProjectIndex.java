package net.arctics.clonk.parser;

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
