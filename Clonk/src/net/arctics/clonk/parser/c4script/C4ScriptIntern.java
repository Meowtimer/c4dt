package net.arctics.clonk.parser.c4script;

import java.io.Serializable;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.Utilities;

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
		setScriptfile(scriptFile);
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
	
	public void setScriptfile(IResource f) throws CoreException {
		if (Utilities.resourceEqual(scriptFile, f))
			return;
		if (scriptFile != null)
			scriptFile.setSessionProperty(ClonkCore.SCRIPT_PROPERTY_ID, null);
		scriptFile = f;
		if (f != null)
			f.setSessionProperty(ClonkCore.SCRIPT_PROPERTY_ID, this);
		scriptFilePath = f != null ? f.getFullPath().toPortableString() : "";
	}
	
	public String getScriptFilePath() {
		return scriptFilePath;
	}
	
	public static C4ScriptIntern scriptCorrespondingTo(IResource resource) throws CoreException {
		return (C4ScriptIntern) resource.getSessionProperty(ClonkCore.SCRIPT_PROPERTY_ID);
	}
	
	@Override
	public IResource getResource() {
		return getScriptFile();
	}

}
