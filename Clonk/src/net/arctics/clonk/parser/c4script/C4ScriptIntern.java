package net.arctics.clonk.parser.c4script;

import java.io.Serializable;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * Standalone-script inside a project.
 */
public class C4ScriptIntern extends C4ScriptBase implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private transient IResource scriptFile;
	private String scriptFilePath;
	
	public C4ScriptIntern(IResource scriptFile) throws CoreException {
		this.name = scriptFile.getName();
		setScriptFile(scriptFile);
	}

	@Override
	public ClonkIndex getIndex() {
		ClonkProjectNature nature = Utilities.getClonkNature(scriptFile);
		return nature != null ? nature.getIndex() : null;
	}

	@Override
	public IResource getScriptFile() {
		return scriptFile;
	}
	
	public void setScriptFile(IResource f) throws CoreException {
		if (Utilities.resourceEqual(scriptFile, f))
			return;
		if (scriptFile != null)
			scriptFile.setSessionProperty(ClonkCore.C4STRUCTURE_PROPERTY_ID, null);
		scriptFile = f;
		if (f != null)
			f.setSessionProperty(ClonkCore.C4STRUCTURE_PROPERTY_ID, this);
		scriptFilePath = f != null ? f.getFullPath().toPortableString() : "";
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

}
