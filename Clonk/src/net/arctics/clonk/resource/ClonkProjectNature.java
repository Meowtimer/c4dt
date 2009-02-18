package net.arctics.clonk.resource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.arctics.clonk.parser.ClonkIndex;
import net.arctics.clonk.parser.ProjectIndex;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * project nature for Clonk projects
 */
public class ClonkProjectNature implements IProjectNature {

	/**
	 * Name of the file the index gets saved in
	 */
	private static final String indexFileName = "indexdata";
	
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
	public ClonkIndex getIndex() {
		if (index == null)
			loadIndex();
		return index;
	}
	
	/**
	 * Saves the index to disk
	 * @throws CoreException
	 */
	public void saveIndex() throws CoreException {
		if (indexDirty) {
			getIndex(); // make sure index is loaded in the first place
			final IFile eclipseFile = project.getFile(indexFileName);
			if (!eclipseFile.exists())
				eclipseFile.create(new ByteArrayInputStream(new byte[] {}), IResource.HIDDEN | IResource.DERIVED, null);
			final File index = eclipseFile.getLocation().toFile();
			try {
				FileOutputStream out = new FileOutputStream(index);
				ObjectOutputStream objStream = new ObjectOutputStream(out);
				objStream.writeObject(getIndex());
				objStream.close();
				out.close();
				eclipseFile.refreshLocal(IResource.DEPTH_ZERO, null);
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
		final IFile indexFile = project.getFile(indexFileName);
		if (!indexFile.exists()) {
			index = new ProjectIndex(project);
			return;
		}
		try {
			InputStream in = indexFile.getContents();
			try {
				ObjectInputStream objStream = new InputStreamRespectingUniqueIDs(in);
				index = (ProjectIndex)objStream.readObject();
				index.setProject(getProject());
				index.fixReferencesAfterSerialization();
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

}