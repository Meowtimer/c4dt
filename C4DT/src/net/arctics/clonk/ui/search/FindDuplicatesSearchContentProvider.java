package net.arctics.clonk.ui.search;

import net.arctics.clonk.c4script.Function;

import org.eclipse.jface.viewers.Viewer;

public class FindDuplicatesSearchContentProvider extends SearchContentProvider {

	private DuplicatesSearchQuery query;
	private FindDuplicatesSearchResult result;
	
	public FindDuplicatesSearchContentProvider(SearchResultPage page, boolean flat) {
		super(page, flat);
	}
	
	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(@SuppressWarnings("rawtypes") Viewer viewer, Object oldInput, Object newInput) {
		result = (FindDuplicatesSearchResult) newInput;
		if (result != null)
			query = result.getQuery();
		else
			query = null;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return query.getDetectedDupes().keySet().toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof Function)
			return query.getDetectedDupes().get(parentElement).toArray();
		else
			return new Object[0];
	}

	@Override
	public Object getParent(Object element) { return null; }
	@Override
	public boolean hasChildren(Object element) { return element instanceof Function; }

}
