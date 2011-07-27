package net.arctics.clonk.ui.navigator;

import java.text.Collator;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.index.Engine;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class ClonkSorter extends ViewerSorter {

	private transient IProject cachedProject;
	private transient Engine cachedEngine;
	private transient Map<String, Integer> colorTagToCategory = new HashMap<String, Integer>();
	
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
	
	private int getSortPriorityIgnoringTags(IResource resource) {
		if (!resource.getProject().equals(cachedProject)) {
			cachedProject = resource.getProject();
			cachedEngine = ClonkProjectNature.getEngine(resource);
		}
		GroupType gt;
		if (cachedEngine != null && (gt = cachedEngine.getGroupTypeForFileName(resource.getName())) != GroupType.OtherGroup) {
			for (int i = 0; i < groupSortOrder.length; i++) {
				if (groupSortOrder[i] == gt)
					return i;
			}
		}
		for(int i = 0; i < sortPriorities.length;i++) {
			if (resource.getName().toLowerCase().endsWith(sortPriorities[i]))
				return i+groupSortOrder.length;
		}
		return sortPriorities.length+1;
	}
	
	private synchronized int getSortPriority(IResource resource) {
		int simplePriority = getSortPriorityIgnoringTags(resource);
		String relatedTag;
		try {
			relatedTag = resource.getPersistentProperty(ColorTagging.COLOR_TAG);
			ColorTagging.rgbForResource(resource);
		} catch (CoreException e) {
			relatedTag = null;
		}
		Integer tagCateg;
		if (relatedTag != null) {
			tagCateg = colorTagToCategory.get(relatedTag);
			if (tagCateg == null)
				colorTagToCategory.put(relatedTag, tagCateg = colorTagToCategory.size());
		} else
			tagCateg = colorTagToCategory.size()+1;
		return tagCateg * (groupSortOrder.length+sortPriorities.length) + simplePriority;
	}
	
	@Override
	public int category(Object element) {
		if (element instanceof Declaration)
			return ((Declaration) element).sortCategory();
		else if (element instanceof IResource)
			return getSortPriority((IResource) element);
		else
			return 0;
	}
	
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		return super.compare(viewer, e1, e2);
	}

}

