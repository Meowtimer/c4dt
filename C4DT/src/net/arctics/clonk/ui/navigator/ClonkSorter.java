package net.arctics.clonk.ui.navigator;

import java.text.Collator;
import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4group.C4Group;
import net.arctics.clonk.c4group.C4Group.GroupType;
import net.arctics.clonk.index.Engine;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class ClonkSorter extends ViewerSorter {

	private transient IProject cachedProject;
	private transient Engine cachedEngine;
	private transient Map<String, Integer> colorTagToCategory = new HashMap<String, Integer>();
	
	final static private String[] sortPriorities = new String[] {".c", ".txt", ".bmp", ".png" , ".wav", ".pal"};  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ 
	final static private C4Group.GroupType[] groupSortOrder = new C4Group.GroupType[] {
		GroupType.FolderGroup, GroupType.ScenarioGroup, GroupType.DefinitionGroup, GroupType.ResourceGroup
	};
	
	public ClonkSorter() {
		super();
	}

	public ClonkSorter(final Collator collator) {
		super(collator);
	}
	
	private int getSortPriorityIgnoringTags(final IResource resource) {
		if (!resource.getProject().equals(cachedProject)) {
			cachedProject = resource.getProject();
			cachedEngine = ClonkProjectNature.engineFromResource(resource);
		}
		GroupType gt;
		if (cachedEngine != null && (gt = cachedEngine.groupTypeForFileName(resource.getName())) != GroupType.OtherGroup)
			for (int i = 0; i < groupSortOrder.length; i++)
				if (groupSortOrder[i] == gt)
					return i;
		for(int i = 0; i < sortPriorities.length;i++)
			if (resource.getName().toLowerCase().endsWith(sortPriorities[i]))
				return i+groupSortOrder.length;
		return sortPriorities.length+1;
	}
	
	private synchronized int getSortPriority(final IResource resource) {
		final int simplePriority = getSortPriorityIgnoringTags(resource);
		String relatedTag;
		try {
			relatedTag = resource.getPersistentProperty(ColorTagging.COLOR_TAG);
			ColorTagging.rgbForResource(resource);
		} catch (final CoreException e) {
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
	public int category(final Object element) {
		if (element instanceof Declaration)
			return ((Declaration) element).sortCategory();
		else if (element instanceof IResource)
			return getSortPriority((IResource) element);
		else
			return 0;
	}
	
	@Override
	public int compare(final Viewer viewer, final Object e1, final Object e2) {
		return super.compare(viewer, e1, e2);
	}

}

