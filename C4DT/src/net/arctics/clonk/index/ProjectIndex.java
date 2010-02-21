package net.arctics.clonk.index;

import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptIntern;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.ExternalLib;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

public class ProjectIndex extends ExternIndex {

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

	public List<ExternalLib> getExternalDependencies() {
		return this.getLibs();
		/*List<ExternalLib> allLibs = ClonkCore.getDefault().getExternIndex().getLibs();
		// no explicit dependencies specified; return all set in preferences
		if (dependencyNames == null)
			return allLibs;
		// filter
		return Utilities.filter(allLibs, new IPredicate<ExternalLib>() {
			public boolean test(ExternalLib lib) {
				return Utilities.collectionContains(dependencyNames, lib.getNodeName());
			}
		});*/
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
		getLibs().clear(); // deprecated - just remove
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

}
