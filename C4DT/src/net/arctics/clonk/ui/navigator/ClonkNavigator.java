package net.arctics.clonk.ui.navigator;

import java.net.URI;
import java.util.Collection;

import net.arctics.clonk.filesystem.C4GroupFileSystem;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.C4GroupType;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.Viewer;

/**
 * content provider for adding virtual (non-file based) nodes to the project explorer
 */
public class ClonkNavigator extends ClonkOutlineProvider {

	public Object[] getChildren(Object element) {
		Object[] baseResources = NO_CHILDREN;
		if (element instanceof IContainer) {
			try {
				createC4GroupLinksIn((IContainer)element);
				baseResources = ((IContainer)element).members();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		// add additional virtual nodes to the project
		if (element instanceof IFile) {
			// list contents of ini and script files
			ScriptBase script = ScriptBase.get((IFile) element, true);
			if (script != null)
				return ArrayUtil.concat(baseResources, super.getChildren(script));
			try {
				Structure s = Structure.pinned((IFile) element, false, false);
				if (s instanceof ITreeNode) {
					// call again for ITreeNode object (below)
					return ArrayUtil.concat(baseResources, this.getChildren(s));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (element instanceof IProject) {
			// actually ... there is nothing to add. move along
		}
		else if (element instanceof ITreeNode) {
			Collection<? extends INode> children = ((ITreeNode)element).getChildCollection();
			return children != null ? ArrayUtil.concat(baseResources, (Object[])children.toArray(new INode[children.size()])) : baseResources;
		}
		return ArrayUtil.concat(baseResources, super.getChildren(element));
	}

	private void createC4GroupLinksIn(IContainer element) {
		try {
			if (element.isLinked())
				return;
			IResource[] resources = element.members();
			NullProgressMonitor mon = null;
			for (IResource res : resources) {
				C4GroupItem groupItem;
				if (res instanceof IFile && C4Group.getGroupType(res.getName()) != C4GroupType.OtherGroup) {
					if (mon == null)
						mon = new NullProgressMonitor();
					groupItem = C4GroupItem.getGroupItemBackingResource(res);
					if (groupItem == null) {
						// not linked to C4Group but some other thingie? - ignore
						if (res.isLinked())
							continue;
						IFile file = (IFile)res;
						IPath resLocation = res.getLocation();
						// evil shtupid hack? link existing file to null filesystem
						file.createLink(new URI(
							EFS.SCHEME_NULL, C4GroupFileSystem.replaceSpecialChars(resLocation.toOSString()), null),
							IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, mon
						);
						// delete it (not deleting the actual file because it's linked)
						file.delete(true, mon);
						// create linked folder
						IFolder folder = element.getFolder(new Path(resLocation.lastSegment()));
						folder.createLink(new URI(
							C4GroupFileSystem.SCHEME,
							C4GroupFileSystem.replaceSpecialChars(resLocation.toOSString()), null),
							0, mon
						);
					}
				} else if (res instanceof IFolder && (groupItem = C4GroupItem.getGroupItemBackingResource(res)) instanceof C4Group) {
					if (!((C4Group)groupItem).existsOnDisk()) {
						res.delete(true, mon);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean hasChildren(Object element) {
		if (element instanceof IContainer) {
			return true;
		}
		else if (element instanceof IFile) {
			ScriptBase script = ScriptBase.get((IFile) element, true);
			if (script != null)
				return super.hasChildren(script);
			try {
				Structure s;
				if ((s = Structure.pinned((IFile) element, true, false)) != null)
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
		return ClonkProjectNature.getClonkProjects();
	}

	public void dispose() {
		
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {	
	}

}
