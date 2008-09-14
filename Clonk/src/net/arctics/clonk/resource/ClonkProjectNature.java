package net.arctics.clonk.resource;

import net.arctics.clonk.parser.ClonkIndexer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class ClonkProjectNature implements IProjectNature {
	
	private ClonkIndexer indexer; // the root c4d file (e.g. Objects.c4d)
	private IProject project;
	private boolean isIndexed = false;
	
	public ClonkProjectNature() {
		indexer = new ClonkIndexer();
	}
	
	public void indexAll() {
		try {
			for(IResource res : project.members()) {
				if (res instanceof IFolder) {
					if (res.getName().startsWith("c4d.") || res.getName().startsWith("c4g."))
						indexer.indexFolder((IFolder)res, project.getFullPath(), true);
				}
			}
			isIndexed = true;
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	public void index(IFile file) {
		indexer.indexFolder((IFolder)file.getParent(), file.getParent().getFullPath(), false);
	}
	
	@Override
	public void configure() throws CoreException {
		
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
	 * @return the compiler
	 */
	public ClonkIndexer getIndexer() {
		return indexer;
	}

	/**
	 * @param compiler the compiler to set
	 */
	public void setIndexer(ClonkIndexer compiler) {
		this.indexer = compiler;
	}

	/**
	 * @return the isIndexed
	 */
	public boolean isIndexed() {
		return isIndexed;
	}


}