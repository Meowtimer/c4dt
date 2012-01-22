package net.arctics.clonk.ui.search;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Script;

import org.eclipse.core.resources.IFile;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;

public abstract class ClonkSearchQueryBase implements ISearchQuery, IFileMatchAdapter, IEditorMatchAdapter {

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
		if (result == null) {
			result = new ClonkSearchResult(this);
		}
		return result;
	}
	
	@Override
	public IFile getFile(Object element) {
		if (element instanceof Declaration)
			return ((Declaration)element).script().getScriptFile();
		if (element instanceof Script)
			return  ((Script)element).getScriptFile();
		if (element instanceof IFile) {
			return (IFile)element;
		}
		return null;
	}

}
