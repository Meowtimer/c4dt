package net.arctics.clonk.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class ClonkProjectNature implements IProjectNature {

	private static final String indexDataFile = "indexdata";
	
	private IProject project;
	
	private ProjectIndex indexedData = null;
	
	public ClonkProjectNature() {
	}
	
	public void configure() throws CoreException {
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
	public ClonkIndex getIndexedData() {
		if (indexedData == null)
			loadIndexData();
		return indexedData;
	}
	
	public void saveIndexData() throws CoreException {
		final IFile eclipseFile = project.getFile(indexDataFile);
		if (!eclipseFile.exists())
			eclipseFile.create(new ByteArrayInputStream(new byte[] {}), IResource.HIDDEN | IResource.DERIVED, null);
		final File index = eclipseFile.getLocation().toFile();
		try {
			FileOutputStream out = new FileOutputStream(index);
			ObjectOutputStream objStream = new ObjectOutputStream(out);
			objStream.writeObject(getIndexedData());
			objStream.close();
			out.close();
			eclipseFile.refreshLocal(IResource.DEPTH_ZERO, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Starts Deserialization into <tt>indexedData</tt>
	 */
	private void loadIndexData() {
		final IFile index = project.getFile(indexDataFile);
		if (!index.exists()) {
			indexedData = new ProjectIndex(project);
			return;
		}
		try {
			InputStream in = index.getContents();
			ObjectInputStream objStream = new InputStreamRespectingUniqueIDs(in);
			indexedData = (ProjectIndex)objStream.readObject();
			indexedData.setProject(getProject());
			indexedData.fixReferencesAfterSerialization();
		} catch (Exception e) {
			e.printStackTrace();
			// somehow failed - ignore
			indexedData = new ProjectIndex(project);
		}
	}

}