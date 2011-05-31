package net.arctics.clonk.ui.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorPart;

public class WildcardSearchQuery extends ClonkSearchQueryBase {

	@Override
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		return null;
	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Match[] computeContainedMatches(AbstractTextSearchResult result,
			IFile file) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Match[] computeContainedMatches(AbstractTextSearchResult result,
			IEditorPart editor) {
		// TODO Auto-generated method stub
		return null;
	}

}
