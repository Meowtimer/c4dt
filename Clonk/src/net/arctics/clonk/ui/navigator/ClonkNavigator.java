package net.arctics.clonk.ui.navigator;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.Viewer;

/**
 * content provider for adding virtual (non-file based) nodes to the project explorer
 */
public class ClonkNavigator extends ClonkOutlineProvider {

	public Object[] getChildren(Object element) {
		if (element instanceof IProject) {
			ClonkProjectNature clonkProject = Utilities.getProject((IProject)element);
			if (clonkProject == null)
				return null;
			return new Object[] { new DependenciesNavigatorNode(clonkProject) };
		}
		else if (element instanceof IFile) {
			C4ScriptBase script = Utilities.getScriptForFile((IFile) element);
			if (script != null)
				return super.getChildren(script);
		}
		else if (element instanceof ITreeNode) {
			List<? extends ITreeNode> children = ((ITreeNode)element).getChildren();
			if (children != null && !children.isEmpty())
			return Utilities.concat(((ITreeNode) element).getChildren().toArray(), super.getChildren(element));
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
		}
		else if (element instanceof ITreeNode) {
			ITreeNode node = (ITreeNode) element;
			if (node.getChildren() != null && node.getChildren().size() > 0)
				return true;
		}
		return super.hasChildren(element);
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
		return super.getElements(inputElement);
	}

	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub
		
	}

}
