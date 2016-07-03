package net.arctics.clonk.ui.search;

import net.arctics.clonk.c4script.Function;

import org.eclipse.jface.viewers.Viewer;

public class FindDuplicatesSearchContentProvider extends SearchContentProvider {

	private DuplicatesSearchQuery query;
	private FindDuplicatesSearchResult result;
	
	public FindDuplicatesSearchContentProvider(final SearchResultPage page, final boolean flat) {
		super(page, flat);
	}
	
	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		result = (FindDuplicatesSearchResult) newInput;
		if (result != null)
			query = result.getQuery();
		else
			query = null;
	}

	@Override
	public Object[] getElements(final Object inputElement) {
		return query.getDetectedDupes().keySet().toArray();
	}

	@Override
	public Object[] getChildren(final Object parentElement) {
		if (parentElement instanceof Function)
			return query.getDetectedDupes().get(parentElement).toArray();
		else
			return new Object[0];
	}

	@Override
	public Object getParent(final Object element) { return null; }
	@Override
	public boolean hasChildren(final Object element) { return element instanceof Function; }

}
