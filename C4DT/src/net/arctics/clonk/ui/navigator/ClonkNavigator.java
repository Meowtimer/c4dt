package net.arctics.clonk.ui.navigator;

import java.net.URI;
import java.util.Collection;

import net.arctics.clonk.filesystem.C4GroupFileSystem;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
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
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.Viewer;

/**
 * content provider for adding virtual (non-file based) nodes to the project explorer
 */
public class ClonkNavigator extends ClonkOutlineProvider {

	private boolean showStructureOutlines() {return ClonkPreferences.getPreferenceToggle(ClonkPreferences.STRUCTURE_OUTLINES_IN_PROJECT_EXPLORER, true);}
	
	@Override
	public Object[] getChildren(Object element) {
		boolean showStructureOutlines = showStructureOutlines();
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
		if (element instanceof IFile && showStructureOutlines) {
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
		else if (element instanceof ITreeNode && showStructureOutlines) {
			Collection<? extends INode> children = ((ITreeNode)element).getChildCollection();
			return children != null ? ArrayUtil.concat(baseResources, (Object[])children.toArray(new INode[children.size()])) : baseResources;
		}
		return showStructureOutlines
			? ArrayUtil.concat(baseResources, super.getChildren(element))
			: baseResources;
	}

	private void createC4GroupLinksIn(IContainer element) {
		try {
			if (element.isLinked())
				return;
			Engine engine = ClonkProjectNature.getEngine(element);
			if (engine == null)
				return;
			IResource[] resources = element.members();
			NullProgressMonitor mon = null;
			for (IResource res : resources) {
				C4GroupItem groupItem;
				if (res instanceof IFile && engine.getGroupTypeForFileName(res.getName()) != GroupType.OtherGroup) {
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

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof IProject && !((IProject)element).isOpen())
			return false;
		boolean s = showStructureOutlines();
		if (element instanceof IContainer) {
			return true;
		}
		else if (element instanceof IFile && s) {
			ScriptBase script = ScriptBase.get((IFile) element, true);
			if (script != null)
				return super.hasChildren(script);
			try {
				Structure structure;
				if ((structure = Structure.pinned((IFile) element, true, false)) != null)
					return structure instanceof ITreeNode && ((ITreeNode)structure).getChildCollection().size() > 0;
			} catch (CoreException e) {
				return false;
			}
		}
		else if (element instanceof ITreeNode && s) {
			ITreeNode node = (ITreeNode) element;
			if (node.getChildCollection() != null && node.getChildCollection().size() > 0)
				return true;
		}
		return s ? super.hasChildren(element) : false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IWorkspaceRoot)
			return ClonkProjectNature.getClonkProjects();
		else
			return getChildren(inputElement);
	}

	@Override
	public void dispose() {
		
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {	
	}

}
