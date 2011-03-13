package net.arctics.clonk.ui.navigator;

import java.text.Collator;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class ClonkSorter extends ViewerSorter {

	private transient IProject cachedProject;
	private transient Engine cachedEngine;
	
	final static private String[] sortPriorities = new String[] {".c", ".txt", ".bmp", ".png" , ".wav", ".pal"};  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
	final static private C4Group.GroupType[] groupSortOrder = new C4Group.GroupType[] {
		GroupType.FolderGroup, GroupType.ScenarioGroup, GroupType.DefinitionGroup, GroupType.ResourceGroup
	};
	
	public ClonkSorter() {
		super();
	}

	public ClonkSorter(Collator collator) {
		super(collator);
	}
	
	private synchronized int getSortPriority(IResource resource) {
		if (resource.getProject() != cachedProject) {
			cachedProject = resource.getProject();
			cachedEngine = ClonkProjectNature.getEngine(resource);
		}
		GroupType gt;
		if (cachedEngine != null && (gt = cachedEngine.getGroupTypeForFileName(resource.getName())) != GroupType.OtherGroup) {
			for (int i = 0; i < groupSortOrder.length; i++) {
				if (groupSortOrder[i] == gt)
					return i - groupSortOrder.length; // sort category negative for folders so they will be always on top
			}
		}
		for(int i = 0; i < sortPriorities.length;i++) {
			if (resource.getName().toLowerCase().endsWith(sortPriorities[i]))
				return i;
		}
		return sortPriorities.length+1;
	}
	
	@Override
	public int category(Object element) {
		if (element instanceof Declaration) {
			return ((Declaration) element).sortCategory();
		}
		if (element instanceof IResource) {
			return getSortPriority((IResource) element);
		}
		return 0;
	}
	
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		return super.compare(viewer, e1, e2);
	}

}

