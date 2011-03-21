package net.arctics.clonk.ui.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

public abstract class ClonkSearchQueryBase implements ISearchQuery {

	protected ClonkSearchResult result;
	
	@Override
	public IStatus run(IProgressMonitor monitor)
			throws OperationCanceledException {
		// TODO Auto-generated method stub
		return null;
	}

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

}
