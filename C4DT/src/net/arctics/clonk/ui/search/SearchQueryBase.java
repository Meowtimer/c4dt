package net.arctics.clonk.ui.search;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.util.IHasRelatedResource;

import org.eclipse.core.resources.IFile;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;

public abstract class SearchQueryBase implements ISearchQuery, IFileMatchAdapter, IEditorMatchAdapter {

	protected static final Match[] NO_MATCHES = new Match[0];
	
	protected ClonkSearchResult result;

	@Override
	public boolean canRerun() {
		return true;
	}

	@Override
	public boolean canRunInBackground() {
		return true;
	}

	@Override
	public ISearchResult getSearchResult() {
		if (result == null)
			result = new ClonkSearchResult(this);
		return result;
	}
	
	@Override
	public IFile getFile(Object element) {
		if (element instanceof Script)
			return  ((Script)element).scriptFile();
		if (element instanceof IFile)
			return (IFile)element;
		if (element instanceof IHasRelatedResource)
			return as(((IHasRelatedResource)element).resource(), IFile.class);
		return null;
	}

}
