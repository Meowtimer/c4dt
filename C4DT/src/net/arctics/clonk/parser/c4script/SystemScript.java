package net.arctics.clonk.parser.c4script;

import java.io.Serializable;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.StreamUtil;
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
public class SystemScript extends ScriptBase implements Serializable {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private transient IFile scriptFile;
	private String scriptFilePath;
	
	public SystemScript(Index index, IFile scriptFile) throws CoreException {
		super(index);
		this.name = scriptFile.getName();
		setScriptFile(scriptFile);
	}

	@Override
	public IFile getScriptStorage() {
		return scriptFile;
	}
	
	@Override
	public String getScriptText() {
		try {
			return scriptFile instanceof IFile ? StreamUtil.stringFromFileDocument((IFile)scriptFile) : null;
		} catch (Exception e) {
			return null;
		}
	}
	
	public void setScriptFile(IFile f) throws CoreException {
		if (Utilities.objectsEqual(scriptFile, f))
			return;
		if (scriptFile != null)
			scriptFile.setSessionProperty(ClonkCore.FILE_STRUCTURE_REFERENCE_ID, null);
		scriptFile = f;
		if (f != null) { 
			f.setSessionProperty(ClonkCore.FILE_STRUCTURE_REFERENCE_ID, this);
			ClonkProjectNature nature = ClonkProjectNature.get(scriptFile);
			index = nature != null ? nature.getIndex() : null;
		}
		scriptFilePath = f != null ? f.getFullPath().toPortableString() : ""; //$NON-NLS-1$
	}
	
	public String getScriptFilePath() {
		return scriptFilePath;
	}
	
	public static SystemScript pinnedScript(IResource resource, boolean duringBuild) throws CoreException {
		Structure s = pinned(resource, false, duringBuild);
		return s instanceof SystemScript ? (SystemScript) s : null;
	}
	
	@Override
	public IResource getResource() {
		return getScriptStorage();
	}
	
	@Override
	public void pinTo(IResource resource) throws CoreException {
		assert(resource instanceof IFile);
		setScriptFile((IFile) resource);
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
	
	public static SystemScript scriptCorrespondingTo(IFile file) {
		ScriptBase script = Utilities.getIndex(file) != null ? Utilities.getIndex(file).getScript(file) : null;
		return script instanceof SystemScript ? (SystemScript)script : null;
	}
	
	@Override
	public Object additionalEntityIdentificationToken() {
		return scriptFilePath;
	}

}
