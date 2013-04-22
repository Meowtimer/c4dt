package net.arctics.clonk.builder;

import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.index.Definition;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

class ResourceCounterAndCleaner extends ResourceCounter {
	public ResourceCounterAndCleaner(int countFlags) {
		super(countFlags);
	}
	@Override
	public boolean visit(IResource resource) throws CoreException {
		if (resource instanceof IContainer) {
			Definition obj = Definition.definitionCorrespondingToFolder((IContainer) resource);
			if (obj != null)
				obj.setDefinitionFolder(null);
		}
		else if (resource instanceof IFile)
			Structure.unPinFrom((IFile) resource);
		return super.visit(resource);
	}
	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta.getKind() == IResourceDelta.CHANGED)
			return super.visit(delta);
		return true;
	}
}