package net.arctics.clonk.ui.search;

public class FindDuplicatesSearchResult extends ClonkSearchResult {

	public FindDuplicatesSearchResult(SearchQueryBase query) {
		super(query);
	}
	
	@Override
	public DuplicatesQuery getQuery() {
		return (DuplicatesQuery) super.getQuery();
	}

}
