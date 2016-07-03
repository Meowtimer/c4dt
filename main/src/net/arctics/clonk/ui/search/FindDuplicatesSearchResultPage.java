package net.arctics.clonk.ui.search;

import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.ui.editors.StructureTextEditor;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.PartInitException;

public class FindDuplicatesSearchResultPage extends SearchResultPage {
	@Override
	protected SearchContentProvider getContentAndLabelProvider(final boolean flat) {
		return new FindDuplicatesSearchContentProvider(this, flat);
	}
	@Override
	protected void showMatch(final Match match, final int currentOffset, final int currentLength, final boolean activate) throws PartInitException {
		final FindDuplicatesMatch m = (FindDuplicatesMatch) match;
		StructureTextEditor.openDeclaration(m.getDupe());
	}
	@Override
	protected void handleOpen(final OpenEvent event) {
		final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		if (selection.getFirstElement() instanceof Function)
			StructureTextEditor.openDeclaration((Function) selection.getFirstElement());
		else
			super.handleOpen(event);
	}
}
