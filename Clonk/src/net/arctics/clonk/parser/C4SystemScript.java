package net.arctics.clonk.parser;

import java.io.Serializable;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Utilities;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class C4SystemScript extends C4ScriptBase implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private transient IResource scriptFile;
	private String scriptFilePath;
	
	public C4SystemScript(IResource scriptFile) throws CoreException {
		this.name = scriptFile.getName();
		this.scriptFile = scriptFile;
		this.scriptFilePath = scriptFile.getFullPath().toPortableString();
		scriptFile.setSessionProperty(ClonkCore.SCRIPT_PROPERTY_ID, this);
	}

	@Override
	public ClonkIndex getIndex() {
		return Utilities.getProject(scriptFile).getIndexedData();
	}

	@Override
	public Object getScriptFile() {
		return scriptFile;
	}
	
	public void setScriptfile(IResource f) throws CoreException {
		if (f == scriptFile)
			return;
		if (scriptFile != null)
			scriptFile.setSessionProperty(ClonkCore.SCRIPT_PROPERTY_ID, null);
		scriptFile = f;
		if (f != null)
			f.setSessionProperty(ClonkCore.SCRIPT_PROPERTY_ID, this);
	}
	
	public String getScriptFilePath() {
		return scriptFilePath;
	}
	
	public static C4SystemScript scriptCorrespondingTo(IResource resource) throws CoreException {
		return (C4SystemScript) resource.getSessionProperty(ClonkCore.SCRIPT_PROPERTY_ID);
	}

}
