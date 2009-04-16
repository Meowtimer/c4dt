package net.arctics.clonk.ui.navigator;

import java.text.Collator;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class ClonkSorter extends ViewerSorter {

	final static private String[] sortPriorities = new String[] {".c4f", ".c4s", ".c4g" , ".txt", ".bmp", ".png", ".c",".c4d", ".wav", ".pal"}; 
	
	public ClonkSorter() {
	}

	public ClonkSorter(Collator collator) {
		super(collator);
	}

	private int getSortPriority(IResource resource) {
		for(int i = 0; i < sortPriorities.length;i++) {
			if (resource.getName().endsWith(sortPriorities[i])) return i;
		}
		return -1;
	}
	
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof DependenciesNavigatorNode)
			return -1; // always first place
		if (e1 instanceof IResource && e2 instanceof IResource) {
			int p1 = getSortPriority((IResource) e1);
			int p2 = getSortPriority((IResource) e2);
			if (p1 >= 0 && p2 >= 0) {
				if (p1 > p2) return 1;
				else if (p2 > p1) return -1;
				else return 0;
			}
		}
		return super.compare(viewer, e1, e2);
	}

}

