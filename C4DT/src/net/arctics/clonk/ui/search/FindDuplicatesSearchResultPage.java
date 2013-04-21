package net.arctics.clonk.ui.search;

import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.ui.editors.ClonkTextEditor;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.PartInitException;

public class FindDuplicatesSearchResultPage extends SearchResultPage {
	@Override
	protected SearchContentProvider getContentAndLabelProvider(boolean flat) {
		return new FindDuplicatesSearchContentProvider(this, flat);
	}
	@Override
	protected void showMatch(Match match, int currentOffset, int currentLength, boolean activate) throws PartInitException {
		FindDuplicatesMatch m = (FindDuplicatesMatch) match;
		ClonkTextEditor.openDeclaration(m.getDupe());
	}
	@Override
	protected void handleOpen(OpenEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		if (selection.getFirstElement() instanceof Function)
			ClonkTextEditor.openDeclaration((Function) selection.getFirstElement());
		else
			super.handleOpen(event);
	}
}
