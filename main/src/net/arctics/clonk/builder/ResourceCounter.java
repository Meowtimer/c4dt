package net.arctics.clonk.builder;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

public class ResourceCounter implements IResourceVisitor, IResourceDeltaVisitor {
	
	private int count;
	private final int flags;
	
	public final static int COUNT_CONTAINER = 1;
	public final static int COUNT_FILE = 2;
	
	public ResourceCounter(final int countFlags) {
		count = 0;
		flags = countFlags;
	}
	
	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

	@Override
	public boolean visit(final IResource resource) throws CoreException {
		if ((flags & COUNT_CONTAINER) > 0) {
			if (resource instanceof IContainer) {
				count++;
			}
		}
		if ((flags & COUNT_FILE) > 0) {
			if (resource instanceof IFile) {
				count++;
			}
		}
		return true;
	}

	@Override
	public boolean visit(final IResourceDelta delta) throws CoreException {
		return visit(delta.getResource());
	}

}
