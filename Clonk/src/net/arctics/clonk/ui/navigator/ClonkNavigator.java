package net.arctics.clonk.ui.navigator;

import java.util.Collection;

import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;

/**
 * content provider for adding virtual (non-file based) nodes to the project explorer
 */
public class ClonkNavigator extends ClonkOutlineProvider {

	public Object[] getChildren(Object element) {
		// supply standard resources -.-
		if (element instanceof IContainer) {
			IContainer container = (IContainer) element;
			try {
				ClonkProjectNature clonkProject = element instanceof IProject ? Utilities.getClonkNature((IProject)element) : null;
				DependenciesNavigatorNode depNode = clonkProject != null ? new DependenciesNavigatorNode(clonkProject) : null;
				return depNode != null ? Utilities.concat((Object[])container.members(), depNode) : container.members();
			} catch (CoreException e) {
				return null;
			}
		}
		// add additional virtual nodes to the project
		else if (element instanceof IProject) {
			ClonkProjectNature clonkProject = Utilities.getClonkNature((IProject)element);
			if (clonkProject == null)
				return null;
			return new Object[] {
				// Dependencies
				new DependenciesNavigatorNode(clonkProject)
			};
		}
		else if (element instanceof IFile) {
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
		else if (element instanceof ITreeNode) {
			Collection<? extends ITreeNode> children = ((ITreeNode)element).getChildCollection();
			if (children != null && !children.isEmpty())
				return Utilities.concat(((ITreeNode) element).getChildCollection().toArray(), super.getChildren(element));
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
		if (inputElement instanceof IWorkspaceRoot) {
			return ((IWorkspaceRoot)inputElement).getProjects();
//			File file = new File(((IWorkspaceRoot)inputElement).getLocationURI());
//			FilenameFilter filter = new FilenameFilter() {
//				private String[] extensions = new String[] { ".c4d", ".c4s", ".c4f", ".c4g" };
// 				public boolean accept(File dir, String name) {
// 					for (int i = 0; i < extensions.length; i++) {
// 						if (name.endsWith(extensions[i]))
// 							return true;
// 					}
// 					return false;
//				}
//			};
//			String[] files = file.list(filter);
//			Object[] projects = new Object[files.length];
//			
//			for(int i = 0;i < files.length;i++) {
//				projects[i] = ResourcesPlugin.getWorkspace().getRoot().getProject(files[i]);
////				((IProject)projects[i]).o
//			}
//			return projects;
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
