package net.arctics.clonk.ui.search;

public class FindDuplicatesSearchResult extends ClonkSearchResult {

	public FindDuplicatesSearchResult(ClonkSearchQueryBase query) {
		super(query);
	}
	
	@Override
	public FindDuplicatesQuery getQuery() {
		return (FindDuplicatesQuery) super.getQuery();
	}

}
