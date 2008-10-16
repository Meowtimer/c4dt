package net.arctics.clonk.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.arctics.clonk.parser.ClonkIndex;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class ClonkProjectNature implements IProjectNature {

	private static final String indexDataFile = "indexdata";
	
//	private ClonkIndexer indexer; // the root c4d file (e.g. Objects.c4d)
	private IProject project;
	private boolean isIndexed = false;
	
	private ClonkIndex indexedData = null;
	
	public ClonkProjectNature() {
//		indexer = new ClonkIndexer();
	}
	
//	public void indexAll() {
//		try {
//			for(IResource res : project.members()) {
//				if (res instanceof IFolder) {
//					C4GroupType groupType = ClonkIndexer.groupTypeFromFolderName(res.getName());
//					if (groupType != C4GroupType.OtherGroup)
//						indexer.indexFolder((IFolder)res, project.getFullPath(), true);
//				}
//			}
//			isIndexed = true;
//		}
//		catch (CoreException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public void index(IFile file) {
//		indexer.indexFolder((IFolder)file.getParent(), file.getParent().getFullPath(), false);
//	}
	
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
	
//	/**
//	 * @return the compiler
//	 */
//	public ClonkIndexer getIndexer() {
//		return indexer;
//	}
//
//	/**
//	 * @param compiler the compiler to set
//	 */
//	public void setIndexer(ClonkIndexer compiler) {
//		this.indexer = compiler;
//	}

	/**
	 * @return the isIndexed
	 */
	public boolean isIndexed() {
		return isIndexed;
	}

	/**
	 * @return the indexedData
	 */
	public ClonkIndex getIndexedData() {
		if (indexedData == null)
			loadIndexData();
		return indexedData;
	}
	
	public void saveIndexData() {
		final IFile index = project.getFile(indexDataFile);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ObjectOutputStream objStream = new ObjectOutputStream(out);
			objStream.writeObject(getIndexedData());
			objStream.close();
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			if (index.exists()) {
				index.setContents(in, true, false, null);
			} else {
				index.create(in, IResource.DERIVED | IResource.HIDDEN | IResource.FORCE, null);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void loadIndexData() {
		final IFile index = project.getFile(indexDataFile);
		if (!index.exists()) {
			indexedData = new ClonkIndex(project);
			return;
		}
		try {
			InputStream in = index.getContents();
			ObjectInputStream objStream = new ObjectInputStream(in);
			indexedData = (ClonkIndex)objStream.readObject();
			indexedData.setProject(getProject());
			indexedData.fixReferences();
		} catch (Exception e) {
			e.printStackTrace();
			// somehow failed - ignore
			indexedData = new ClonkIndex(project);
		}
	}


}