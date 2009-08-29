package net.arctics.clonk.ui.navigator;

import java.util.Collection;


import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ExternIndex;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;

/**
 * content provider for adding virtual (non-file based) nodes to the project explorer
 */
public class ClonkNavigator extends ClonkOutlineProvider {

	public Object[] getChildren(Object element) {
		// add additional virtual nodes to the project
		if (element instanceof IFile) {
			// list contents of ini and script files
			C4ScriptBase script = Utilities.getScriptForFile((IFile) element);
			if (script != null)
				return super.getChildren(script);
			try {
				C4Structure s = C4Structure.pinned((IFile) element, false);
				if (s instanceof ITreeNode) {
					// call again for ITreeNode object (below)
					return this.getChildren(s);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (element instanceof IProject) {
			if (Utilities.getDependenciesProject() == element) {
				ExternIndex index = ClonkCore.getDefault().getExternIndex();
				return index.getLibs().toArray(new Object[index.getLibs().size()]);
			}
		}
		else if (element instanceof ITreeNode) {
			Collection<? extends INode> children = ((ITreeNode)element).getChildCollection();
			return children != null ? children.toArray(new INode[children.size()]) : NO_CHILDREN;
		}
		return super.getChildren(element);
	}

	public boolean hasChildren(Object element) {
		if (element instanceof IProject) {
			if (((IProject)element).isOpen())
				return true;
		}
		else if (element instanceof IFile) {
			C4ScriptBase script = Utilities.getScriptForFile((IFile) element);
			if (script != null)
				return super.hasChildren(script);
			try {
				C4Structure s;
				if ((s = C4Structure.pinned((IFile) element, true)) != null)
					return (s instanceof ITreeNode && ((ITreeNode)s).getChildCollection().size() > 0);
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
