package net.arctics.clonk.parser.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.io.Serializable;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

/**
 * Standalone-script inside a project.
 */
public class SystemScript extends Script implements Serializable {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private transient IFile scriptFile;
	private String scriptFilePath;
	
	public SystemScript(Index index, IFile scriptFile) throws CoreException {
		super(index);
		this.name = scriptFile.getName();
		setScriptFile(scriptFile);
	}

	@Override
	public IFile scriptStorage() {
		return scriptFile;
	}
	
	@Override
	public String scriptText() {
		try {
			return scriptFile instanceof IFile ? StreamUtil.stringFromFileDocument(scriptFile) : null;
		} catch (Exception e) {
			return null;
		}
	}
	
	@Override
	public void setScriptFile(IFile f) {
		if (Utilities.objectsEqual(scriptFile, f))
			return;
		if (scriptFile != null)
			super.unPinFrom(scriptFile);
		scriptFile = f;
		if (f != null) { 
			super.pinTo(scriptFile);
			ClonkProjectNature nature = ClonkProjectNature.get(scriptFile);
			index = nature != null ? nature.index() : null;
		}
		scriptFilePath = f != null ? f.getProjectRelativePath().toPortableString() : ""; //$NON-NLS-1$
	}
	
	public String scriptFilePath() {
		return scriptFilePath;
	}
	
	public static SystemScript pinned(IResource resource, boolean duringBuild) {
		Structure s = Structure.pinned(resource, true, duringBuild);
		return as(s, SystemScript.class);
	}
	
	@Override
	public IResource resource() {
		return scriptStorage();
	}
	
	@Override
	public void pinTo(IResource resource) {
		assert(resource instanceof IFile);
		setScriptFile((IFile) resource);
	}

	public boolean refreshFileReference(IProject project) throws CoreException {
		Path projectPath = new Path(scriptFilePath());
		IResource res = project.findMember(projectPath);
		if (res instanceof IFile) {
			setScriptFile((IFile) res);
			return true;
		}
		else
			return false;
	}
	
	public static SystemScript scriptCorrespondingTo(IFile file) {
		ProjectIndex index = ProjectIndex.fromResource(file);
		Script script = index != null ? index.scriptAt(file) : null;
		return script instanceof SystemScript ? (SystemScript)script : null;
	}
	
	@Override
	public Object additionalEntityIdentificationToken() {
		return scriptFilePath;
	}
	
	public static void register() {
		registerStructureFactory(new IStructureFactory() {
			@Override
			public Structure create(IResource resource, boolean duringBuild) {
				if (!resource.getName().endsWith(".c"))
					return null;
				ProjectIndex index = ProjectIndex.fromResource(resource);
				if (index != null)
					for (Script script : index.indexedScripts()) {
						SystemScript sysScript = as(script, SystemScript.class);
						if (sysScript != null && sysScript.scriptFile() != null && sysScript.scriptFile().equals(resource)) {
							try {
								index.loadEntity(sysScript);
							} catch (Exception e) {
								e.printStackTrace();
							}
							return sysScript;
						}
					}
				return null;
			}
		});
	}

}
