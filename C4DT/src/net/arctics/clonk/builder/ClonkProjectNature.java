package net.arctics.clonk.builder;

import static net.arctics.clonk.util.ArrayUtil.map;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import net.arctics.clonk.Core;
import net.arctics.clonk.Milestones;
import net.arctics.clonk.ast.AppendableBackedExprWriter;
import net.arctics.clonk.builder.ProjectSettings.ProblemReportingStrategyInfo;
import net.arctics.clonk.c4script.ProblemReportingStrategy;
import net.arctics.clonk.c4script.ProblemReportingStrategy.Capabilities;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.SystemScript;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.ini.CustomIniUnit;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.StreamUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;

/**
 * project nature for Clonk projects
 */
public class ClonkProjectNature implements IProjectNature {

	private final Object lock = new Object();

	public static final Function<ClonkProjectNature, Index> SELECT_INDEX = nature -> nature.index();
	public static final Function<IProject, ClonkProjectNature> SELECT_NATURE = project -> get(project);

	public static ClonkProjectNature[] allInWorkspace() {
		return map(clonkProjectsInWorkspace(), ClonkProjectNature.class, SELECT_NATURE);
	}

	public Set<ClonkProjectNature> projectSet() {
		@SuppressWarnings("serial")
		class ProjectSet extends HashSet<ClonkProjectNature> {
			void addRecursive(final ClonkProjectNature n) {
				if (add(n))
					for (final IProject p : n.getProject().getReferencingProjects()) {
						final ClonkProjectNature referencing = get(p);
						if (referencing != null)
							addRecursive(referencing);
					}
			}
			ProjectSet(final ClonkProjectNature start) {
				addRecursive(start);
			}
		}
		return new ProjectSet(this);
	}

	/**
	 * Reference to the project
	 */
	private IProject project;

	/**
	 *  index of the project
	 */
	private ProjectIndex index = null;

	/**
	 * Set when loadIndex is taking place. Used for preventing infinite recursion.
	 */
	private boolean indexLoadingPending = false;

	/**
	 * Settings stored in ini file
	 */
	private ProjectSettings settings;

	public ClonkProjectNature() {}

	@Override
	public void configure() throws CoreException {}
	@Override
	public void deconfigure() throws CoreException {}
	@Override
	public IProject getProject() { return project; }
	@Override
	public void setProject(final IProject project) { this.project = project; }
	public List<ProblemReportingStrategy> problemReportingStrategies() {
		return instantiateProblemReportingStrategies(0);
	}

	/**
	 * Returns the index of the project, loading it from disk if necessary.
	 * @return the project index
	 */
	public ProjectIndex index() {
		synchronized (lock) {
			if (index == null && !indexLoadingPending) {
				indexLoadingPending = true;
				try {
					load();
				} finally {
					indexLoadingPending = false;
				}
			}
			return index;
		}
	}

	/**
	 * Release the current {@link #index} and create a new one. This ensures that newly-added fields are properly initialized. Called by {@link ClonkBuilder#clean(IProgressMonitor)}.
	 * @return The newly created index.
	 */
	public ProjectIndex forceIndexRecreation() {
		synchronized (lock) {
			index = null;
			loadSettings();
			final File indexFolder = indexFolder();
			// legacy index file - delete
			if (indexFolder.isFile())
				indexFolder.delete();
			return index = new ProjectIndex(project, indexFolder);
		}
	}

	public IPath settingsFilePath() {
		return Core.instance().getStateLocation().append(getProject().getName()+".ini");
	}

	/**
	 * Saves the index to disk
	 * @throws CoreException
	 */
	public void saveIndex() throws CoreException {
		if (index != null) {
			saveSettings();
			index.saveShallow();
		}
	}

	public void saveSettings() {
		try {
			settings();
			StreamUtil.writeToFile(settingsFilePath().toFile(), (file, stream, writer) -> {
				try {
					CustomIniUnit.save(new AppendableBackedExprWriter(writer), settings, null);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			});
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void loadSettings() {
		try {
			settings = new ProjectSettings();
			CustomIniUnit.load(StreamUtil.stringFromFile(settingsFilePath().toFile()), settings);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load the index from disk. Exceptions thrown while loading cause a new empty index to be created and returned.
	 */
	private void load() {
		settings();
		try {
			ProjectIndex loadedIndex = Index.loadShallow(ProjectIndex.class, indexFolder(), null, settings().engine());
			if (loadedIndex != null) {
				index = loadedIndex; // necessary to avoid infinite recursion
				loadedIndex.setProject(getProject());
				try {
					loadedIndex.postLoad();
				} catch (final CoreException e) {
					e.printStackTrace();
					loadedIndex = null;
				}
			}
			index = loadedIndex != null ? loadedIndex : new ProjectIndex(getProject(), indexFolder());
		} catch (final Exception e) {
			System.out.println(String.format("Loading index for %s failed - performing clean build", this.getProject().getName()));
			performCleanBuild();
		}
	}

	private void performCleanBuild() {
		Display.getDefault().asyncExec(() -> {
			final IProgressMonitor monitor = new NullProgressMonitor();
			try {
				project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
			} catch (final CoreException e) {
				e.printStackTrace();
			}
			if (Core.instance().versionFromLastRun().compareTo(Milestones.VERSION_THAT_INTRODUCED_PROJECT_SETTINGS) < 0)
				settings().guessValues(ClonkProjectNature.this);
		});
	}

	private File indexFolder() {
		return Core.instance().getStateLocation().append(getProject().getName()+ProjectIndex.INDEXFILE_SUFFIX).toFile();
	}

	/**
	 * Returns the clonk project nature associated with the project of res
	 * @param res the resource
	 * @return the nature
	 */
	public static ClonkProjectNature get(final IResource res) {
		if (res == null)
			return null;
		final IProject project = res.getProject();
		try {
			if (project == null || !project.isOpen() || !project.hasNature(Core.NATURE_ID))
				return null;
		} catch (final CoreException e1) {
			return null;
		}
		try {
			final IProjectNature clonkProj = project.getNature(Core.NATURE_ID);
			return (ClonkProjectNature) clonkProj;
		} catch (final CoreException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get the clonk nature of the project the given script is contained in
	 * @param script the script
	 * @return the nature
	 */
	public static ClonkProjectNature get(final Script script) {
		if (script == null)
			return null;
		if (script instanceof Definition)
			return get(((Definition)script).definitionFolder());
		if (script instanceof SystemScript)
			return get(((SystemScript)script).source());
		else
			return null;
	}

	public static ClonkProjectNature get(final String projectName) {
		for (final IProject proj : clonkProjectsInWorkspace())
			if (proj.getName().equals(projectName))
				try {
					return (ClonkProjectNature) proj.getNature(Core.NATURE_ID);
				} catch (final CoreException e) {
					e.printStackTrace();
					return null;
				}
		return null;
	}

	public ProjectSettings settings() {
		synchronized (lock) {
			if (settings == null)
				loadSettings();
		}
		return settings;
	}

	/**
	 * All Clonk projects in the current workspace
	 * @return array containing the Clonk projects
	 */
	public static IProject[] clonkProjectsInWorkspace() {
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject[] projects = root.getProjects();

		// Filter out all projects with Clonk nature
		final Collection<IProject> c = new LinkedList<IProject>();
		for(final IProject proj : projects)
			if (ClonkProjectNature.get(proj) != null)
				c.add(proj);

		return c.toArray(new IProject [c.size()]);
	}

	private static void addProjectsFromReferencedProjects(final List<IProject> result, final IProject proj) {
		try {
			final List<IProject> newOnes = new LinkedList<IProject>();
			for (final IProject p : proj.getReferencedProjects()) {
				final ClonkProjectNature n = ClonkProjectNature.get(p);
				if (n != null && !newOnes.contains(p))
					newOnes.add(p);
			}
			result.addAll(newOnes);
			for (final IProject i : newOnes)
				addProjectsFromReferencedProjects(result, i);
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}

	public List<IProject> referencingClonkProjects() {
		final List<IProject> result = new ArrayList<IProject>(10);
		result.add(getProject());
		addProjectsFromReferencedProjects(result, getProject());
		return result;
	}

	public void reloadIndex() {
		index = null;
		index();
	}

	public static Engine engineFromResource(final IResource res) {
		final ClonkProjectNature nat = get(res);
		return nat != null ? nat.settings().engine() : null;
	}

	public static Engine engineFromSelection(final ISelection selection) {
		if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).getFirstElement() instanceof IResource)
			return engineFromResource((IResource) ((IStructuredSelection)selection).getFirstElement());
		else
			return null;
	}

	private static final List<ProblemReportingStrategy> NULL_PROBLEM_REPORTERS = Arrays.<ProblemReportingStrategy>asList(new NullProblemReportingStrategy());

	public List<ProblemReportingStrategy> instantiateProblemReportingStrategies(final int requiredCapabilities) {
		try {
			if (!ClonkPreferences.toggle(ClonkPreferences.ANALYZE_CODE, true))
				return NULL_PROBLEM_REPORTERS;
			final Collection<ProblemReportingStrategyInfo> classes = settings().problemReportingStrategies();
			final List<ProblemReportingStrategy> instances = new ArrayList<ProblemReportingStrategy>(classes.size());
			for (final ProblemReportingStrategyInfo c : classes) {
				final Capabilities caps = c.cls.getAnnotation(Capabilities.class);
				if (caps == null || (caps.capabilities() & requiredCapabilities) != requiredCapabilities)
					continue;
				try {
					final ProblemReportingStrategy instance = c.cls.newInstance();
					instance.configure(index(), c.args);
					instances.add(instance);
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
					continue;
				}
			}
			return instances;
		} catch (final Exception e) {
			return Collections.emptyList();
		}
	}

}
