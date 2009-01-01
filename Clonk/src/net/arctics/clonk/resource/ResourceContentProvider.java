package net.arctics.clonk.resource;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ResourceContentProvider implements ITreeContentProvider {
	
	private IResource root;
	private int resourceTypes;
	
	public ResourceContentProvider(IResource root) {
		this(root, 0x0);
	}
	
	public ResourceContentProvider(IResource root, int resourceTypes) {
		this.root = root;
		this.resourceTypes = resourceTypes;
	}
	
	public Object[] getChildren(Object parentElement) {
		return getElements(parentElement);
	}

	public Object getParent(Object element) {
		if (element instanceof IResource) {
			return ((IResource)element).getParent();
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof IContainer) {
			try {
				if (!((IResource)element).getProject().isOpen()) return false;
				IResource[] members = ((IContainer)element).members();
				for(IResource member : members) {
					if ((member.getType() & resourceTypes) > 0) {
						return true;
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IContainer) {
			try {
				List<IResource> children = new ArrayList<IResource>();
				IResource[] members = ((IContainer)inputElement).members();
				for(IResource member : members) {
					if (member.getName().startsWith(".")) continue;
					if ((member.getType() & resourceTypes) > 0) {
						children.add(member);
					}
				}
				return children.toArray(new IResource[children.size()]);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof IResource)
			root = (IResource) newInput;
	}

}
