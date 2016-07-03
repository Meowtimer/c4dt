package net.arctics.clonk.builder;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.index.Definition;

class ResourceCounterAndCleaner extends ResourceCounter {
	public ResourceCounterAndCleaner(final int countFlags) {
		super(countFlags);
	}
	@Override
	public boolean visit(final IResource resource) throws CoreException {
		if (resource instanceof IContainer) {
			final Definition obj = Definition.at((IContainer) resource);
			if (obj != null) {
				obj.setDefinitionFolder(null);
			}
		}
		else if (resource instanceof IFile) {
			Structure.unPinFrom((IFile) resource);
		}
		return super.visit(resource);
	}
	@Override
	public boolean visit(final IResourceDelta delta) throws CoreException {
		if (delta.getKind() == IResourceDelta.CHANGED) {
			return super.visit(delta);
		}
		return true;
	}
}