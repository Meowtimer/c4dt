package net.arctics.clonk.ui.navigator;

import java.text.Collator;

import net.arctics.clonk.parser.C4Declaration;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class ClonkSorter extends ViewerSorter {

	final static private String[] sortPriorities = new String[] {".c4f", ".c4s", ".c4d", ".c4g" , ".c", ".txt", ".bmp", ".png" , ".wav", ".pal"}; 
	
	public ClonkSorter() {
		super();
	}

	public ClonkSorter(Collator collator) {
		super(collator);
	}
	
	private int getSortPriority(IResource resource) {
		for(int i = 0; i < sortPriorities.length;i++) {
			if (resource.getName().toLowerCase().endsWith(sortPriorities[i]))
				return i;
		}
		return sortPriorities.length+1;
	}
	
	@Override
	public int category(Object element) {
		if (element instanceof C4Declaration) {
			return ((C4Declaration) element).sortCategory();
		}
		if (element instanceof IResource) {
			return getSortPriority((IResource) element);
		}
		return 0;
	}
	
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof DependenciesNavigatorNode)
			return -1; // always first place
		if (e1 instanceof GlobalDeclarationsNavigatorNode)
			return -1;
		return super.compare(viewer, e1, e2);
	}

}

