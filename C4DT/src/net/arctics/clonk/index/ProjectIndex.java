package net.arctics.clonk.index;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptIntern;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.parser.playercontrols.PlayerControlsUnit;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

public class ProjectIndex extends ClonkIndex {

	private static final long serialVersionUID = 1L;
	public static final String INDEXFILE_SUFFIX = ".index"; //$NON-NLS-1$
	
	private String engineName;
	
	private transient IProject project;
	private transient boolean isDirty;
	private transient C4Engine cachedEngine;
	
	public String getEngineName() {
		return engineName;
	}
	
	public void setEngineName(String engineName) {
		this.engineName = engineName;
		this.cachedEngine = null;
		setDirty(true);
	}
	
	@Override
	public C4Engine getEngine() {
		if (cachedEngine == null) {
			// engineName can be "" or null since that is handled by loadEngine
			cachedEngine = ClonkCore.getDefault().loadEngine(engineName);
			if (cachedEngine == null)
				cachedEngine = ClonkCore.getDefault().getActiveEngine();
		}
		return cachedEngine;
	}
	
	public ClonkProjectNature getNature() {
		return ClonkProjectNature.get(project);
	}
	
	public ProjectIndex(IProject project) {
		this.project = project;
	}
	
	public void setProject(IProject proj) {
		project = proj;
	}
	
	public IProject getProject() {
		return project;
	}

	public void postSerialize() throws CoreException {
		if (project != null) {
			List<C4ScriptBase> stuffToBeRemoved = new LinkedList<C4ScriptBase>();
			for (C4Object object : this) {
				if (object instanceof C4ObjectIntern) {
					if (!((C4ObjectIntern)object).refreshFolderReference(project)) {
						stuffToBeRemoved.add(object);
					}
				}
			}
			for (C4Scenario scenario : getIndexedScenarios()) {
				if (!scenario.refreshFolderReference(project)) {
					stuffToBeRemoved.add(scenario);
				}
			}
			for (C4ScriptBase script : getIndexedScripts()) {
				if (script instanceof C4ScriptIntern) {
					C4ScriptIntern standalone = (C4ScriptIntern) script;
					if (!standalone.refreshFileReference(project)) {
						stuffToBeRemoved.add(standalone);
					}
				}
			}
			// purge objects that seem to be non-existent
			for (C4ScriptBase s : stuffToBeRemoved) {
				this.removeScript(s);
			}
		}
		super.postSerialize();
	}
	
	@Override
	public String toString() {
		return project != null ? "Index for " + project.toString() : "Orphan Project Index"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	@Override
	public void setDirty(boolean dirty) {
		isDirty = dirty;
	}
	
	@Override
	public boolean isDirty() {
		return isDirty;
	}
	
	@Override
	public C4ScriptBase findScriptByPath(String path) {
		IResource res = getProject().findMember(new Path(path));
		if (res != null) {
			C4ScriptBase result;
			try {
				result = Utilities.getScriptForResource(res);
				if (result != null)
					return result;
			} catch (CoreException e) {
				e.printStackTrace();
				return null;
			}
		}
		return super.findScriptByPath(path);
	}
	
	public static ProjectIndex get(IProject project) {
		ClonkProjectNature nature = ClonkProjectNature.get(project);
		return nature != null ? nature.getIndex() : null;
	}
	
	@Override
	public synchronized void refreshIndex() {
		super.refreshIndex();
		
		try {
			getProject().accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IContainer) {
						return true;
					} else if (resource instanceof IFile && resource.getName().equals("PlayerControls.txt")) {
						PlayerControlsUnit unit = (PlayerControlsUnit) IniUnit.pinned(resource, true, true);
						if (unit != null) {
							staticVariables.addAll(unit.getControlVariables());
							for (C4Variable v : unit.getControlVariables())
								addToDeclarationMap(v);
						}
						return true;
					}
					else
						return false;
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}
