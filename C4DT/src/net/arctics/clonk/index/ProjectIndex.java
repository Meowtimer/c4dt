package net.arctics.clonk.index;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SystemScript;
import net.arctics.clonk.c4script.typing.Typing;
import net.arctics.clonk.util.ObjectFinderVisitor;
import net.arctics.clonk.util.Sink;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
	public Engine engine() { return nature.settings().engine(); }
	/**
	 * Return the {@link ClonkProjectNature} this index belongs to. This is a shorthand for {@link ClonkProjectNature}.get({@link #nature()})
	 * @return The {@link ClonkProjectNature}
	 */
	@Override
	public ClonkProjectNature nature() { return nature; }

	/**
	 * Initialize a new ProjectIndex for the given project.
	 * @param project The project to initialize the index for
	 */
	public ProjectIndex(final IProject project, final File folder) {
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
	public void setProject(final IProject proj) {
		project = proj;
		nature = ClonkProjectNature.get(project);
	}

	@Override
	public void postLoad() throws CoreException {
		super.postLoad();
		if (project != null) {
			final List<Script> stuffToBeRemoved = new LinkedList<Script>();
			allDefinitions(new Sink<Definition>() {
				@Override
				public void receivedObject(final Definition item) {
					stuffToBeRemoved.add(item);
				}
				@Override
				public boolean filter(final Definition item) {
					return !item.refreshDefinitionFolderReference(project);
				}
			});
			for (final Scenario scenario : scenarios())
				if (!scenario.refreshDefinitionFolderReference(project))
					stuffToBeRemoved.add(scenario);
			for (final Script script : scripts())
				if (script instanceof SystemScript) {
					final SystemScript standalone = (SystemScript) script;
					if (!standalone.refreshFileReference(project))
						stuffToBeRemoved.add(standalone);
				}
			// purge objects that seem to be non-existent
			for (final Script s : stuffToBeRemoved)
				this.removeScript(s);
		}
	}

	@Override
	public String toString() {
		return project != null ? "Index for " + project.toString() : "Orphan Project Index"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Find a script belonging to the project resource denoted by the given path.
	 */
	@Override
	public Script findScriptByPath(final String pathString) {
		synchronized (nature) {
			final Path path = new Path(pathString);
			final IResource res = findScriptFileMatchingPath(path);
			if (res != null) {
				final Script result = Utilities.scriptForResource(res);
				if (result != null)
					return result;
			}
			return super.findScriptByPath(pathString);
		}
	}
	
	private IResource findScriptFileMatchingPath(final Path path) {
		final IResource perfect = nature().getProject().findMember(path);
		if (perfect != null)
			return perfect;
		for (final Scenario s : scenarios()) {
			final IContainer scenarioFolder = s.definitionFolder();
			if (scenarioFolder != null && path.segment(0).equals(scenarioFolder.getName())) {
				final IResource res = scenarioFolder.findMember(path.removeFirstSegments(1));
				if (res != null)
					return res;
			}
		}
		return null;
	}

	/**
	 * Return the project index for the given project.
	 * @param project The project to return the ProjectIndex of
	 * @return The ProjectIndex
	 */
	public static ProjectIndex get(final IProject project) {
		final ClonkProjectNature nature = ClonkProjectNature.get(project);
		return nature != null ? nature.index() : null;
	}

	@Override
	public void refresh(final boolean postLoad) {
		super.refresh(postLoad);
		engine().specialRules().refreshIndex(this);
	}

	public <T extends Structure> T findPinnedStructure(final Class<T> cls, final String name, final IResource pivot, final boolean create, final String fileName) {
		final ObjectFinderVisitor<T> finder = new ObjectFinderVisitor<T>() {
			@SuppressWarnings("unchecked")
			@Override
			public boolean visit(final IResource resource) throws CoreException {
				if (!resource.getName().equals(fileName))
					return true;
				final Structure s = Structure.pinned(resource, create, false);
				if (s != null && cls.isAssignableFrom(s.getClass()) && s.name().equals(name)) {
					result = (T) s;
					return false;
				}
				return true;
			}
		};
		final List<T> r = new LinkedList<T>();
		for (final Index i : relevantIndexes())
			if (i instanceof ProjectIndex) {
				finder.reset();
				try {
					((ProjectIndex)i).nature().getProject().accept(finder);
				} catch (final CoreException e) {
					e.printStackTrace();
					continue;
				}
				if (finder.result() != null)
					r.add(finder.result());
			}
		return Utilities.pickNearest(r, pivot, null);
	}

	public static ProjectIndex fromResource(final IResource res) {
		if (res != null) {
			final ClonkProjectNature nature = ClonkProjectNature.get(res);
			if (nature != null)
				return nature.index();
		}
		return null;
	}

	@Override
	public Typing typing() { return nature().settings().typing; }

}
