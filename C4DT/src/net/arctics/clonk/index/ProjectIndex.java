package net.arctics.clonk.index;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.SystemScript;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.playercontrols.PlayerControlsUnit;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.ObjectFinderVisitor;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

/**
 * A {@link Index} geared towards storing information about Eclipse Clonk projects.<br\>
 * A project index is obtained from some project file using {@link #get(IProject)}.</p>
 */
public class ProjectIndex extends Index {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public static final String INDEXFILE_SUFFIX = ".index"; //$NON-NLS-1$
	
	private transient IProject project;
	private transient ClonkProjectNature nature;
	
	@Override
	public Engine engine() {
		return nature.settings().engine();
	}
	
	/**
	 * Return the {@link ClonkProjectNature} this index belongs to. This is a shorthand for {@link ClonkProjectNature}.get({@link #project()})
	 * @return The {@link ClonkProjectNature}
	 */
	public final ClonkProjectNature getNature() {
		return nature;
	}
	
	/**
	 * Initialize a new ProjectIndex for the given project.
	 * @param project The project to initialize the index for
	 */
	public ProjectIndex(IProject project, File folder) {
		super(folder);
		setProject(project);
	}
	
	/**
	 * Overriding {@link Declaration#name()} ensures that the project's name and the name of the index will be in sync.
	 */
	@Override
	public String name() {
		setName(project.getName());
		return super.name();
	}
	
	/**
	 * Set the project the index belongs to.
	 * @param proj The project
	 */
	public void setProject(IProject proj) {
		project = proj;
		nature = ClonkProjectNature.get(project);
	}
	
	/**
	 * Return the project the index belongs to.
	 */
	@Override
	public IProject project() {
		return project;
	}

	@Override
	public void postLoad() throws CoreException {
		if (project != null) {
			final List<Script> stuffToBeRemoved = new LinkedList<Script>();
			allDefinitions(new Sink<Definition>() {
				@Override
				public void receivedObject(Definition item) {
					stuffToBeRemoved.add(item);
				}
				@Override
				public boolean filter(Definition item) {
					return !item.refreshDefinitionFolderReference(project);
				}
			});
			for (Scenario scenario : indexedScenarios())
				if (!scenario.refreshDefinitionFolderReference(project))
					stuffToBeRemoved.add(scenario);
			for (Script script : indexedScripts())
				if (script instanceof SystemScript) {
					SystemScript standalone = (SystemScript) script;
					if (!standalone.refreshFileReference(project)) {
						stuffToBeRemoved.add(standalone);
					}
				}
			// purge objects that seem to be non-existent
			for (Script s : stuffToBeRemoved)
				this.removeScript(s);
		}
		super.postLoad();
	}
	
	@Override
	public String toString() {
		return project != null ? "Index for " + project.toString() : "Orphan Project Index"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Find a script belonging to the project resource denoted by the given path. 
	 */
	@Override
	public Script findScriptByPath(String path) {
		IResource res = project().findMember(new Path(path));
		if (res != null) {
			Script result;
			try {
				result = Utilities.scriptForResource(res);
				if (result != null)
					return result;
			} catch (CoreException e) {
				e.printStackTrace();
				return null;
			}
		}
		return super.findScriptByPath(path);
	}
	
	/**
	 * Return the project index for the given project.
	 * @param project The project to return the ProjectIndex of
	 * @return The ProjectIndex
	 */
	public static ProjectIndex get(IProject project) {
		ClonkProjectNature nature = ClonkProjectNature.get(project);
		return nature != null ? nature.index() : null;
	}

	@Override
	public synchronized void refreshIndex(boolean postLoad) {
		super.refreshIndex(postLoad);
		readVariablesFromPlayerControlsFile();
	}

	private void readVariablesFromPlayerControlsFile() {
		try {
			project().accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IContainer) {
						return true;
					} else if (resource instanceof IFile && resource.getName().equals("PlayerControls.txt")) { //$NON-NLS-1$
						PlayerControlsUnit unit = (PlayerControlsUnit) Structure.pinned(resource, true, true);
						if (unit != null) {
							staticVariables.addAll(unit.controlVariables());
							for (Variable v : unit.controlVariables())
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
	
	public <T extends Structure> T findPinnedStructure(final Class<T> cls, final String name, IResource pivot, final boolean create, final String fileName) {
		ObjectFinderVisitor<T> finder = new ObjectFinderVisitor<T>() {
			@SuppressWarnings("unchecked")
			@Override
			public boolean visit(IResource resource) throws CoreException {
				if (!resource.getName().equals(fileName))
					return true;
				Structure s = Structure.pinned(resource, create, false);
				if (s != null && cls.isAssignableFrom(s.getClass()) && s.name().equals(name)) {
					result = (T) s;
					return false;
				}
				return true;
			}
		};
		List<T> r = new LinkedList<T>();
		for (Index i : relevantIndexes()) {
			if (i instanceof ProjectIndex) {
				finder.reset();
				try {
					((ProjectIndex)i).project().accept(finder);
				} catch (CoreException e) {
					e.printStackTrace();
					continue;
				}
				if (finder.result() != null) {
					r.add(finder.result());
				}
			}
		}
		return Utilities.pickNearest(r, pivot, null);
	}

	public static ProjectIndex fromResource(IResource res) {
		if (res != null) {
			ClonkProjectNature nature = ClonkProjectNature.get(res);
			if (nature != null)
				return nature.index();
		}
		return null;
	}

}
