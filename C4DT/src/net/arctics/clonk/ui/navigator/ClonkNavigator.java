package net.arctics.clonk.ui.navigator;

import java.net.URI;
import java.util.Collection;

import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.C4Group;
import net.arctics.clonk.c4group.C4GroupFileSystem;
import net.arctics.clonk.c4group.C4GroupItem;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.preferences.ClonkPreferences;
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
 * content provider for adding virtual nodes to the project explorer, which include outlines of scripts and virtual {@link EFS} file nodes of packed C4Groups (see {@link C4GroupFileSystem}).
 */
public class ClonkNavigator extends ClonkOutlineProvider {

	public ClonkNavigator() { super(); }

	private boolean showStructureOutlines() {return ClonkPreferences.toggle(ClonkPreferences.STRUCTURE_OUTLINES_IN_PROJECT_EXPLORER, true);}

	@Override
	public Object[] getChildren(final Object element) {
		if (element instanceof IResource && !((IResource)element).getProject().isOpen())
			return NO_CHILDREN;
		final boolean showStructureOutlines = showStructureOutlines();
		Object[] baseResources = NO_CHILDREN;
		if (element instanceof IContainer)
			try {
				createC4GroupLinksIn((IContainer)element);
				baseResources = ((IContainer)element).members();
			} catch (final CoreException e) {
				e.printStackTrace();
			}
		// add additional virtual nodes to the project
		if (element instanceof IFile && showStructureOutlines) {
			// list contents of ini and script files
			final Script script = Script.get((IFile) element, true);
			if (script != null)
				return ArrayUtil.concat(baseResources, super.getChildren(script));
			try {
				final Structure s = Structure.pinned((IFile) element, false, false);
				if (s instanceof ITreeNode)
					// call again for ITreeNode object (below)
					return ArrayUtil.concat(baseResources, this.getChildren(s));
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		else if (element instanceof ITreeNode && showStructureOutlines) {
			final Collection<? extends INode> children = ((ITreeNode)element).childCollection();
			return children != null ? ArrayUtil.concat(baseResources, (Object[])children.toArray(new INode[children.size()])) : baseResources;
		}
		return showStructureOutlines
			? ArrayUtil.concat(baseResources, super.getChildren(element))
			: baseResources;
	}

	private void createC4GroupLinksIn(final IContainer element) {
		try {
			if (element.isLinked())
				return;
			final Engine engine = ClonkProjectNature.engineFromResource(element);
			if (engine == null)
				return;
			final IResource[] resources = element.members();
			NullProgressMonitor mon = null;
			for (final IResource res : resources) {
				C4GroupItem groupItem;
				if (res instanceof IFile && engine.extensionForFileName(res.getName()).group()) {
					if (mon == null)
						mon = new NullProgressMonitor();
					groupItem = C4GroupItem.groupItemBackingResource(res);
					if (groupItem == null) {
						// not linked to C4Group but some other thingie? - ignore
						if (res.isLinked())
							continue;
						final IFile file = (IFile)res;
						final IPath resLocation = res.getLocation();
						// evil shtupid hack? link existing file to null filesystem
						file.createLink(new URI(
							EFS.SCHEME_NULL, C4GroupFileSystem.replaceSpecialChars(resLocation.toOSString()), null),
							IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, mon
						);
						// delete it (not deleting the actual file because it's linked)
						file.delete(true, mon);
						// create linked folder
						final IFolder folder = element.getFolder(new Path(resLocation.lastSegment()));
						folder.createLink(new URI(
							C4GroupFileSystem.SCHEME,
							C4GroupFileSystem.replaceSpecialChars(resLocation.toOSString()), null),
							0, mon
						);
					}
				} else if (res instanceof IFolder && (groupItem = C4GroupItem.groupItemBackingResource(res)) instanceof C4Group)
					if (!((C4Group)groupItem).existsOnDisk())
						res.delete(true, mon);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean hasChildren(final Object element) {
		if (element instanceof IProject && !((IProject)element).isOpen())
			return false;
		final boolean s = showStructureOutlines();
		if (element instanceof IContainer)
			return true;
		else if (element instanceof IFile && s) {
			final Script script = Script.get((IFile) element, true);
			if (script != null)
				return super.hasChildren(script);
			Structure structure;
			if ((structure = Structure.pinned((IFile) element, true, false)) != null)
				return structure instanceof ITreeNode && ((ITreeNode)structure).childCollection().size() > 0;
		}
		else if (element instanceof ITreeNode && s) {
			final ITreeNode node = (ITreeNode) element;
			if (node.childCollection() != null && node.childCollection().size() > 0)
				return true;
		}
		return s ? super.hasChildren(element) : false;
	}

	@Override
	public Object[] getElements(final Object inputElement) {
		if (inputElement instanceof IWorkspaceRoot)
			return ClonkProjectNature.clonkProjectsInWorkspace();
		else
			return getChildren(inputElement);
	}

	@Override
	public void dispose() {}
	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}

}
