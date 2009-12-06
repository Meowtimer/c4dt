package net.arctics.clonk.ui.navigator;

import java.util.Collection;
import java.util.List;


import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.ExternalLib;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;

/**
 * content provider for adding virtual (non-file based) nodes to the project explorer
 */
public class ClonkNavigator extends ClonkOutlineProvider {
	
	public Object[] getChildren(Object element) {
		Object[] baseResources = NO_CHILDREN;
		if (element instanceof IContainer) {
			try {
				baseResources = ((IContainer)element).members();
			} catch (CoreException e) {}
		}
		// add additional virtual nodes to the project
		if (element instanceof IFile) {
			// list contents of ini and script files
			C4ScriptBase script = Utilities.getScriptForFile((IFile) element);
			if (script != null)
				return Utilities.concat(baseResources, super.getChildren(script));
			try {
				C4Structure s = C4Structure.pinned((IFile) element, false, false);
				if (s instanceof ITreeNode) {
					// call again for ITreeNode object (below)
					return Utilities.concat(baseResources, this.getChildren(s));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (element instanceof IProject) {
			try {
				if (((IProject) element).hasNature(ClonkCore.CLONK_DEPS_NATURE_ID)) {
					ClonkProjectNature clonkProj = ClonkProjectNature.get((IResource)element);
					List<ExternalLib> deps = clonkProj != null
						? clonkProj.getDependencies()
						: ClonkCore.getDefault().getExternIndex().getLibs();
					return Utilities.concat(baseResources, deps.toArray(new Object[deps.size()]));
				}
			} catch (CoreException e) {}
		}
		else if (element instanceof ITreeNode) {
			Collection<? extends INode> children = ((ITreeNode)element).getChildCollection();
			return children != null ? Utilities.concat(baseResources, (Object[])children.toArray(new INode[children.size()])) : baseResources;
		}
		return Utilities.concat(baseResources, super.getChildren(element));
	}

	public boolean hasChildren(Object element) {
		if (element instanceof IContainer) {
			return true;
		}
		else if (element instanceof IFile) {
			C4ScriptBase script = Utilities.getScriptForFile((IFile) element);
			if (script != null)
				return super.hasChildren(script);
			try {
				C4Structure s;
				if ((s = C4Structure.pinned((IFile) element, true, false)) != null)
					return s instanceof ITreeNode && ((ITreeNode)s).getChildCollection().size() > 0;
			} catch (CoreException e) {
				return false;
			}
		}
		else if (element instanceof ITreeNode) {
			ITreeNode node = (ITreeNode) element;
			if (node.getChildCollection() != null && node.getChildCollection().size() > 0)
				return true;
		}
		return super.hasChildren(element);
	}

	public Object[] getElements(Object inputElement) {
		return Utilities.getClonkProjects();
	}

	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub
		
	}

}
