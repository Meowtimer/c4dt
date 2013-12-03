package net.arctics.clonk.ui.search;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.util.IHasRelatedResource;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

public abstract class SearchQuery implements ISearchQuery, IFileMatchAdapter, IEditorMatchAdapter {
	protected static final Match[] NO_MATCHES = new Match[0];
	protected SearchResult result;
	@Override
	public boolean canRerun() { return true; }
	@Override
	public boolean canRunInBackground() { return true; }
	@Override
	public synchronized ISearchResult getSearchResult() {
		if (result == null)
			result = new SearchResult(this);
		return result;
	}
	protected abstract IStatus doRun(IProgressMonitor monitor) throws OperationCanceledException;
	@Override
	public IStatus run(final IProgressMonitor monitor) throws OperationCanceledException {
		try {
			return doRun(monitor);
		} finally {
			if (result != null)
				result.clearScanners();
		}
	}
	@Override
	public IFile getFile(final Object element) {
		if (element instanceof Script)
			return  ((Script)element).file();
		if (element instanceof IFile)
			return (IFile)element;
		if (element instanceof IHasRelatedResource)
			return as(((IHasRelatedResource)element).resource(), IFile.class);
		return null;
	}
	@Override
	public Match[] computeContainedMatches(final AbstractTextSearchResult result, final IFile file) {
		final Script script = Script.get(file, true);
		if (script != null)
			return result.getMatches(script);
		return NO_MATCHES;
	}
	@Override
	public boolean isShownInEditor(final Match match, final IEditorPart editor) {
		if (editor instanceof ITextEditor) {
			final Script script = Utilities.scriptForEditor(editor);
			if (script != null && match.getElement().equals(script.source()))
				return true;
		}
		return false;
	}
	@Override
	public Match[] computeContainedMatches(final AbstractTextSearchResult result, final IEditorPart editor) {
		if (editor instanceof ITextEditor) {
			final Script script = Utilities.scriptForEditor(editor);
			if (script != null)
				return result.getMatches(script);
		}
		return NO_MATCHES;
	}
}
