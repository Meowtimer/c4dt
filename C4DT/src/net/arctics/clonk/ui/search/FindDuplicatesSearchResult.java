package net.arctics.clonk.ui.search;

public class FindDuplicatesSearchResult extends SearchResult {

	public FindDuplicatesSearchResult(SearchQuery query) {
		super(query);
	}
	
	@Override
	public DuplicatesSearchQuery getQuery() {
		return (DuplicatesSearchQuery) super.getQuery();
	}

}
