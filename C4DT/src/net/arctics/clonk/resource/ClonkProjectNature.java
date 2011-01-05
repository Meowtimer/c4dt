package net.arctics.clonk.resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
import java.util.Set;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.Milestones;
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptIntern;
import net.arctics.clonk.parser.inireader.CustomIniUnit;
import net.arctics.clonk.parser.inireader.IniField;
import net.arctics.clonk.util.SettingsBase;
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
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * project nature for Clonk projects
 */
public class ClonkProjectNature implements IProjectNature {

	public static class Settings extends SettingsBase {

		@IniField
		public String engineName;
		@IniField
		public String disabledErrors;
		
		private C4Engine cachedEngine;
		private Set<ParserErrorCode> disabledErrorsSet;
		
		public C4Engine getEngine() {
			if (cachedEngine == null) {
				// engineName can be "" or null since that is handled by loadEngine
				cachedEngine = ClonkCore.getDefault().loadEngine(engineName);
				if (cachedEngine == null)
					cachedEngine = ClonkCore.getDefault().getActiveEngine();
			}
			return cachedEngine;
		}
		
		public Set<ParserErrorCode> getDisabledErrorsSet() {
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

		public String getEngineName() {
			return engineName;
		}
		
		public void setEngineName(String engineName) {
			this.engineName = engineName;
			cachedEngine = null;
		}
		
		public void setDisabledErrors(String disabledErrors) {
			this.disabledErrors = disabledErrors;
			disabledErrorsSet = null;
		}

		public String getDisabledErrors() {
			return disabledErrors;
		}

		public void guessValues(ClonkProjectNature nature) {
			guessEngine(nature);
		}

		private void guessEngine(ClonkProjectNature nature) {
			List<IProject> referencingProjects = nature.getReferencingClonkProjects();
			Map<String, Integer> score = new HashMap<String, Integer>();
			for (String engine : ClonkCore.getDefault().getAvailableEngines()) {
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
	 * Settings stored in ini file
	 */
	private Settings settings = new Settings();

	public ClonkProjectNature() {
	}

	public void configure() throws CoreException {
		System.out.println("ClonkProjectNature.configure"); //$NON-NLS-1$
	}

	public void deconfigure() throws CoreException {
	}

	public IProject getProject() {
		return project;
	}

	public void setProject(IProject project) {
		this.project = project;
	}

	/**
	 * Returns the complete index of the project.
	 * @return the indexedData
	 */
	public ProjectIndex getIndex() {
		if (index == null) {
			loadIndex();
		}
		return index;
	}

	public IPath getIndexFileLocation() {
		return ClonkCore.getDefault().getStateLocation().append(getProject().getName()+ProjectIndex.INDEXFILE_SUFFIX);
	}
	
	public IPath getSettingsFileLocation() {
		return ClonkCore.getDefault().getStateLocation().append(getProject().getName()+".ini");
	}
	
	/**
	 * Saves the index to disk
	 * @throws CoreException
	 */
	public void saveIndex() throws CoreException {
		if (index != null) {
			saveSettings();
			if (index.isDirty()) {
				//getIndex(); // make sure index is loaded in the first place -- does not happen
				IPath indexLocation = getIndexFileLocation();
				File indexFile = indexLocation.toFile();
				try {
					FileOutputStream out = new FileOutputStream(indexFile);
					try {
						ObjectOutputStream objStream = new ObjectOutputStream(out);
						getIndex().preSerialize();
						objStream.writeObject(getIndex());
						objStream.close();
					} finally {
						out.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				index.setDirty(false);
			}
		}
	}

	private void saveSettings() {
		try {
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
			CustomIniUnit.load(StreamUtil.stringFromFile(getSettingsFileLocation().toFile()), settings);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads the index from disk
	 */
	private synchronized void loadIndex() {
		loadSettings();
		if (ClonkCore.getDefault().updateTookPlace()) {
			System.out.println(String.format("Update took place: Cleaning project %s", this.project.getName()));
			index = new ProjectIndex(getProject());
			IProgressMonitor monitor = new NullProgressMonitor();
			try {
				project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
			} catch (CoreException e) {
				e.printStackTrace();
			}
			if (ClonkCore.getDefault().getVersionFromLastRun().compareTo(Milestones.VERSION_THAT_INTRODUCED_PROJECT_SETTINGS) < 0) {
				getSettings().guessValues(this);
			}
		} else {
			ProjectIndex loadedIndex = ClonkIndex.load(ProjectIndex.class, getIndexFileLocation().toFile(), null);
			if (loadedIndex != null) {
				index = loadedIndex; // necessary to avoid infinite recursion
				loadedIndex.setProject(getProject());
				try {
					loadedIndex.postSerialize();
				} catch (CoreException e) {
					e.printStackTrace();
					loadedIndex = null;
				}
			}
			index = loadedIndex != null ? loadedIndex : new ProjectIndex(getProject());
		}
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
	public static ClonkProjectNature get(C4ScriptBase script) {
		if (script == null)
			return null;
		if (script instanceof C4ObjectIntern)
			return get(((C4ObjectIntern)script).getObjectFolder());
		if (script instanceof C4ScriptIntern)
			return get(((C4ScriptIntern)script).getScriptStorage());
		else
			return null;
	}

	public Settings getSettings() {
		getIndex(); // trigger loading of index, which includes loading the settings
		return settings;
	}
	
	/**
	 * All Clonk projects in the current workspace
	 * @return array containing the Clonk projects
	 */
	public static IProject[] getClonkProjects() {
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

	public List<IProject> getReferencingClonkProjects() {
		List<IProject> result = new ArrayList<IProject>(10);
		result.add(getProject());
		addProjectsFromReferencedProjects(result, getProject());
		return result;
	}

}