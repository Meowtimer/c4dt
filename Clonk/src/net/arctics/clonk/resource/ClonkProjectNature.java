package net.arctics.clonk.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ProjectIndex;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

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
		System.out.println("ClonkProjectNature.configure");
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
		File indexFile = getIndexFileLocation().toFile();
		if (!indexFile.exists()) {
			index = new ProjectIndex(project);
			return;
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

}