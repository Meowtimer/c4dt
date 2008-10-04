package net.arctics.clonk.resource;

import net.arctics.clonk.parser.ClonkIndex;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class ClonkProjectNature implements IProjectNature {
	
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
		if (indexedData == null) indexedData = new ClonkIndex(project);
		return indexedData;
	}


}