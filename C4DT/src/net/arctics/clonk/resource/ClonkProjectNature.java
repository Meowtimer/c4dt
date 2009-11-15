package net.arctics.clonk.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4ObjectIntern;
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

	/**
	 * Signals whether the index needs to be saved to disk
	 */
	private boolean indexDirty = false;

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
	
	private IPath getOldIndexFileLocation() {
		return project.getFile("indexdata").getLocation(); //$NON-NLS-1$
	}

	/**
	 * Saves the index to disk
	 * @throws CoreException
	 */
	public void saveIndex() throws CoreException {
		if (indexDirty) {
			getIndex(); // make sure index is loaded in the first place
			IPath indexLocation = getIndexFileLocation();
			File index = indexLocation.toFile();
			try {
				FileOutputStream out = new FileOutputStream(index);
				try {
					ObjectOutputStream objStream = new ObjectOutputStream(out);
					objStream.writeObject(getIndex());
					objStream.close();
				} finally {
					out.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			indexDirty = false;
		}
	}

	/**
	 * Loads the index from disk
	 */
	private void loadIndex() {
		boolean oldLocation = false;
		File indexFile = getIndexFileLocation().toFile();
		if (!indexFile.exists()) {
			// fall back to old indexdata file
			indexFile = getOldIndexFileLocation().toFile();
			if (!indexFile.exists()) {
				index = new ProjectIndex(project);
				return;
			}
			else
				oldLocation = true;
		}
		try {
			InputStream in = new FileInputStream(indexFile);
			try {
				ObjectInputStream objStream = new InputStreamRespectingUniqueIDs(in);
				try {
					index = (ProjectIndex)objStream.readObject();
					index.setProject(getProject());
					index.postSerialize();
				} finally {
					objStream.close();
				}
			} finally {
				in.close();
			}
			if (oldLocation) {
				// old location: mark as dirty so it will be saved in the new location when shutting down
				// also remove old file
				markAsDirty();
				indexFile.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
			// somehow failed - ignore
			index = new ProjectIndex(project);
		}
	}

	/**
	 * @return whether the index is dirty and needs to be resaved or not
	 */
	public boolean isIndexDirty() {
		return indexDirty;
	}

	public void markAsDirty() {
		indexDirty = true;
	}

	public List<ExternalLib> getDependencies() {
		return getIndex().getDependencies();
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