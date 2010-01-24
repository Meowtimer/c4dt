package net.arctics.clonk.resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptIntern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * project nature for Clonk projects
 */
public class ClonkProjectNature implements IProjectNature {

	/**
	 * Reference to the project
	 */
	private IProject project;

	/**
	 *  index of the project
	 */
	private ProjectIndex index = null;

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
		if (index == null)
			loadIndex();
		return index;
	}

	public IPath getIndexFileLocation() {
		return ClonkCore.getDefault().getStateLocation().append(getProject().getName()+ProjectIndex.INDEXFILE_SUFFIX);
	}
	
	/**
	 * Saves the index to disk
	 * @throws CoreException
	 */
	public void saveIndex() throws CoreException {
		if (index != null && index.isDirty()) {
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

	/**
	 * Loads the index from disk
	 */
	private synchronized void loadIndex() {
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

	public List<ExternalLib> getExternalDependencies() {
		return getIndex().getExternalDependencies();
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
			return get(((C4ScriptIntern)script).getScriptFile());
		else
			return null;
	}

}