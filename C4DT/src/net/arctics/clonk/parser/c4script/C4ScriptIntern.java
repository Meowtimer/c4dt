package net.arctics.clonk.parser.c4script;

import java.io.Serializable;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Standalone-script inside a project.
 */
public class C4ScriptIntern extends C4ScriptBase implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private transient IFile scriptFile;
	private String scriptFilePath;
	private transient ClonkIndex index;
	
	public C4ScriptIntern(IFile scriptFile) throws CoreException {
		this.name = scriptFile.getName();
		setScriptFile(scriptFile);
	}

	@Override
	public ClonkIndex getIndex() {
		return index;
	}

	@Override
	public IFile getScriptFile() {
		return scriptFile;
	}
	
	@Override
	public String getScriptText() {
		try {
			return Utilities.stringFromFile(getScriptFile());
		} catch (Exception e) {
			return null;
		}
	}
	
	public void setScriptFile(IFile f) throws CoreException {
		if (Utilities.resourceEqual(scriptFile, f))
			return;
		if (scriptFile != null)
			scriptFile.setSessionProperty(ClonkCore.C4STRUCTURE_PROPERTY_ID, null);
		scriptFile = f;
		if (f != null) { 
			f.setSessionProperty(ClonkCore.C4STRUCTURE_PROPERTY_ID, this);
			ClonkProjectNature nature = ClonkProjectNature.get(scriptFile);
			index = nature != null ? nature.getIndex() : null;
		}
		scriptFilePath = f != null ? f.getFullPath().toPortableString() : ""; //$NON-NLS-1$
	}
	
	public String getScriptFilePath() {
		return scriptFilePath;
	}
	
	public static C4ScriptIntern pinnedScript(IFile resource) throws CoreException {
		C4Structure s = pinned(resource, false);
		return s instanceof C4ScriptIntern ? (C4ScriptIntern) s : null;
	}
	
	@Override
	public IResource getResource() {
		return getScriptFile();
	}
	
	@Override
	public void pinTo(IFile file) throws CoreException {
		setScriptFile(file);
	}

	public boolean refreshFileReference(IProject project) throws CoreException {
		Path path = new Path(getScriptFilePath());
		IPath projectPath = path.removeFirstSegments(1);
		IResource res = project.findMember(projectPath);
		if (res instanceof IFile) {
			setScriptFile((IFile) res);
			return true;
		}
		else
			return false;
	}

}
