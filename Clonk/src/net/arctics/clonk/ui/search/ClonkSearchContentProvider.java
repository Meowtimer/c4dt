package net.arctics.clonk.ui.search;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ClonkSearchContentProvider implements ITreeContentProvider {

	private ClonkSearchResult searchResult;
	public ClonkSearchContentProvider(ClonkSearchResultPage page) {
		super();
	}

	public Object[] getChildren(Object element) {
		return searchResult.getMatches(element);
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		return searchResult.getMatchCount(element) > 0;		
	}

	public Object[] getElements(Object input) {
		return searchResult.getElements();
	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		searchResult = (ClonkSearchResult) newInput;
	}

}
