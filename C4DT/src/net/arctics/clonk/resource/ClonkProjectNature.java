package net.arctics.clonk.resource;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Milestones;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.SystemScript;
import net.arctics.clonk.parser.inireader.CustomIniUnit;
import net.arctics.clonk.parser.inireader.IniField;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.util.SettingsBase;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StringUtil;

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
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * project nature for Clonk projects
 */
public class ClonkProjectNature implements IProjectNature {

	public static class ProjectSettings extends SettingsBase {

		@IniField
		public String engineName;
		@IniField
		public String disabledErrors;
		
		private Engine cachedEngine;
		private HashSet<ParserErrorCode> disabledErrorsSet;
		
		public ProjectSettings() {
		}
		
		public Engine getEngine() {
			if (cachedEngine == null) {
				// engineName can be "" or null since that is handled by loadEngine
				cachedEngine = ClonkCore.instance().loadEngine(engineName);
				if (cachedEngine == null)
					cachedEngine = ClonkCore.instance().getActiveEngine();
			}
			return cachedEngine;
		}
		
		public HashSet<ParserErrorCode> getDisabledErrorsSet() {
			if (disabledErrorsSet == null) {
				disabledErrorsSet = new HashSet<ParserErrorCode>();
				if (!disabledErrors.equals("")) {
					String ds[] = disabledErrors.split(",");
					for (String d : ds) {
						try {
							disabledErrorsSet.add(ParserErrorCode.valueOf(d));
						} catch (IllegalArgumentException e) {
							System.out.println("Unknown parser error: " + d);
						}
					}
				}
			}
			return disabledErrorsSet;
		}
		
		public void setDisabledErrors(String disabledErrors) {
			this.disabledErrors = disabledErrors;
			disabledErrorsSet = null;
		}
		
		public void setDisabledErrorsSet(HashSet<ParserErrorCode> errorCodes) {
			this.disabledErrorsSet = errorCodes;
			if (errorCodes != null)
				this.disabledErrors = StringUtil.writeBlock(null, "", "", ",", errorCodes);
		}

		public String getDisabledErrors() {
			return disabledErrors;
		}

		public String getEngineName() {
			return engineName;
		}
		
		public void setEngineName(String engineName) {
			this.engineName = engineName;
			cachedEngine = null;
		}

		public void guessValues(ClonkProjectNature nature) {
			guessEngine(nature);
		}

		private void guessEngine(ClonkProjectNature nature) {
			List<IProject> referencingProjects = nature.referencingClonkProjects();
			Map<String, Integer> score = new HashMap<String, Integer>();
			for (String engine : ClonkCore.instance().namesOfAvailableEngines()) {
				score.put(engine, 0);
			}
			for (IProject proj : referencingProjects) {
				String projName = proj.getName();
				String engine;
				if (projName.equalsIgnoreCase("OPENCLONK") || projName.equalsIgnoreCase("OC")) {
					engine = "OpenClonk";
				} else if (projName.equalsIgnoreCase("ClonkRage") || projName.equalsIgnoreCase("CR")) {
					engine = "ClonkRage";
				} else {
					continue;
				}
				score.put(engine, score.get(engine)+1);
			}
			Entry<String, Integer> best = null;
			for (Entry<String, Integer> entry : score.entrySet()) {
				if (best == null || entry.getValue() > best.getValue()) {
					best = entry;
				}
			}
			setEngineName(best.getKey());
		}
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

	public ClonkProjectNature() {
	}

	@Override
	public void configure() throws CoreException {
		System.out.println("ClonkProjectNature.configure"); //$NON-NLS-1$
	}

	@Override
	public void deconfigure() throws CoreException {
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

	/**
	 * Returns the index of the project, loading it from disk if necessary.
	 * @return the project index
	 */
	public synchronized ProjectIndex index() {
		if (index == null && !indexLoadingPending) {
			indexLoadingPending = true;
			loadIndex();
			indexLoadingPending = false;
		}
		return index;
	}
	
	/**
	 * Release the current {@link #index} and create a new one. This ensures that newly-added fields are properly initialized. Called by {@link ClonkBuilder#clean(IProgressMonitor)}.
	 * @return The newly created index.
	 */
	public ProjectIndex forceIndexRecreation() {
		index = null;
		loadSettings();
		File indexFolder = indexFolder();
		// legacy index file - delete
		if (indexFolder.isFile())
			indexFolder.delete();
		return index = new ProjectIndex(project, indexFolder);
	}
	
	public ProjectIndex getIndexCreatingEmptyOneIfNotPresent() {
		if (index == null) {
			loadSettings();
			index = new ProjectIndex(project, indexFolder()); 
		}
		return index;
	}

	public IPath getSettingsFileLocation() {
		return ClonkCore.instance().getStateLocation().append(getProject().getName()+".ini");
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
			StreamUtil.writeToFile(getSettingsFileLocation().toFile(), new StreamUtil.StreamWriteRunnable() {
				@Override
				public void run(File file, OutputStream stream, OutputStreamWriter writer) throws IOException {
					try {
						CustomIniUnit.save(writer, settings, null);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadSettings() {
		try {
			settings = new ProjectSettings();
			CustomIniUnit.load(StreamUtil.stringFromFile(getSettingsFileLocation().toFile()), settings);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load the index from disk. Exceptions thrown while loading cause a new empty index to be created and returned.
	 */
	private void loadIndex() {
		settings();
		if (ClonkCore.instance().updateTookPlace()) {
			System.out.println(String.format("Update took place: Cleaning project %s", this.project.getName()));
			index = new ProjectIndex(getProject(), indexFolder());
			performBuildOnOutdatedProject();
		} else {
			ProjectIndex loadedIndex = Index.loadShallow(ProjectIndex.class, indexFolder(), null);
			if (loadedIndex != null) {
				index = loadedIndex; // necessary to avoid infinite recursion
				loadedIndex.setProject(getProject());
				try {
					loadedIndex.postLoad();
				} catch (CoreException e) {
					e.printStackTrace();
					loadedIndex = null;
				}
			}
			index = loadedIndex != null ? loadedIndex : new ProjectIndex(getProject(), indexFolder());
		}
	}

	private void performBuildOnOutdatedProject() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				IProgressMonitor monitor = new NullProgressMonitor();
				try {
					project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
				} catch (CoreException e) {
					e.printStackTrace();
				}
				if (ClonkCore.instance().versionFromLastRun().compareTo(Milestones.VERSION_THAT_INTRODUCED_PROJECT_SETTINGS) < 0) {
					settings().guessValues(ClonkProjectNature.this);
				}
			}
		});
	}

	private File indexFolder() {
		return ClonkCore.instance().getStateLocation().append(getProject().getName()+ProjectIndex.INDEXFILE_SUFFIX).toFile();
	}
	
	/**
	 * Returns the clonk project nature of the project the file that is being edited using the supplied editor belongs to
	 * @param editor the editor
	 * @return the nature
	 */
	public static ClonkProjectNature get(ITextEditor editor) {
		if (editor.getEditorInput() instanceof FileEditorInput) {
			return get(((FileEditorInput)editor.getEditorInput()).getFile());
		}
		return null;
	}
	
	/**
	 * Returns the clonk project nature associated with the project of res
	 * @param res the resource
	 * @return the nature
	 */
	public static ClonkProjectNature get(IResource res) {
		if (res == null) return null;
		IProject project = res.getProject();
		try {
			if (project == null || !project.isOpen() || !project.hasNature(ClonkCore.CLONK_NATURE_ID))
				return null;
		} catch (CoreException e1) {
			return null;
		}
		try {
			IProjectNature clonkProj = project.getNature(ClonkCore.CLONK_NATURE_ID);
			return (ClonkProjectNature) clonkProj;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Get the clonk nature of the project the given script is contained in
	 * @param script the script
	 * @return the nature
	 */
	public static ClonkProjectNature get(Script script) {
		if (script == null)
			return null;
		if (script instanceof Definition)
			return get(((Definition)script).definitionFolder());
		if (script instanceof SystemScript)
			return get(((SystemScript)script).scriptStorage());
		else
			return null;
	}
	
	public static ClonkProjectNature get(String projectName) {
		for (IProject proj : clonkProjectsInWorkspace()) {
			if (proj.getName().equals(projectName)) {
				try {
					return (ClonkProjectNature) proj.getNature(ClonkCore.CLONK_NATURE_ID);
				} catch (CoreException e) {
					e.printStackTrace();
					return null;
				}
			}
		}
		return null;
	}
	
	public static ClonkProjectNature get(IWorkbenchPart part) {
		ISelection selection = part.getSite().getSelectionProvider().getSelection();
		if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).getFirstElement() instanceof IResource)
			return get((IResource)((IStructuredSelection)selection).getFirstElement());
		else if (part instanceof ClonkTextEditor)
			return ClonkProjectNature.get(((ClonkTextEditor)part).topLevelDeclaration().index().getProject());
		return null;
	}

	public ProjectSettings settings() {
		if (settings == null)
			loadSettings();
		return settings;
	}
	
	/**
	 * All Clonk projects in the current workspace
	 * @return array containing the Clonk projects
	 */
	public static IProject[] clonkProjectsInWorkspace() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		
		// Filter out all projects with Clonk nature
		Collection<IProject> c = new LinkedList<IProject>();
		for(IProject proj : projects)
			if (ClonkProjectNature.get(proj) != null)
				c.add(proj);
			
		return c.toArray(new IProject [c.size()]);
	}

	private static void addProjectsFromReferencedProjects(List<IProject> result, IProject proj) {
		try {
			List<IProject> newOnes = new LinkedList<IProject>();
			for (IProject p : proj.getReferencedProjects()) {
				ClonkProjectNature n = ClonkProjectNature.get(p);
				if (n != null && !newOnes.contains(p)) {
					newOnes.add(p);
				}
			}
			result.addAll(newOnes);
			for (IProject i : newOnes) {
				addProjectsFromReferencedProjects(result, i);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public List<IProject> referencingClonkProjects() {
		List<IProject> result = new ArrayList<IProject>(10);
		result.add(getProject());
		addProjectsFromReferencedProjects(result, getProject());
		return result;
	}
	
	public void reloadIndex() {
		index = null;
		index();
	}

	public static Engine engineFromResource(IResource res) {
		ClonkProjectNature nat = get(res);
		return nat != null ? nat.settings().getEngine() : null;
	}
	
	public static Engine engineFromSelection(ISelection selection) {
		if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).getFirstElement() instanceof IResource)
			return engineFromResource((IResource) ((IStructuredSelection)selection).getFirstElement());
		else
			return null;
	}

}