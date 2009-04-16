package net.arctics.clonk.ui.navigator;

import java.io.File;
import java.io.FilenameFilter;

import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * content provider for adding virtual (non-file based) nodes to the project explorer
 */
public class ClonkNavigator implements ITreeContentProvider {

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IProject) {
			ClonkProjectNature clonkProject = Utilities.getProject((IProject)parentElement);
			if (clonkProject == null)
				return null;
			return new Object[] { new DependenciesNavigatorNode(clonkProject) };
		}
		else if (parentElement instanceof ITreeNode) {
			return ((ITreeNode) parentElement).getChildren().toArray();
		}
		return null;
	}

	public Object getParent(Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof IProject) {
			if (((IProject)element).isOpen())
				return true;
		}
		else if (element instanceof ITreeNode) {
			ITreeNode node = (ITreeNode) element;
			return node.getChildren() != null && node.getChildren().size() > 0;
		}
		return false;
	}

	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IWorkspaceRoot) {
			File file = new File(((IWorkspaceRoot)inputElement).getLocationURI());
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if (name.endsWith(".c4d")) return true;
					else return false;
				}
			};
			String[] files = file.list(filter);
			Object[] projects = new Object[files.length];
			
			for(int i = 0;i < files.length;i++) {
				projects[i] = ResourcesPlugin.getWorkspace().getRoot().getProject(files[i]);
//				((IProject)projects[i]).o
			}
			return projects;
		}
		return null;
	}

	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub
		
	}

}
