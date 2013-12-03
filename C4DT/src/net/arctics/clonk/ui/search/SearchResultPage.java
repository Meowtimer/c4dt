package net.arctics.clonk.ui.search;

import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.ui.editors.StructureTextEditor;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;

public class SearchResultPage extends AbstractTextSearchViewPage implements IShowInSource, IShowInTargetList {
	
	@Override
	protected void clear() {
		// yep
	}

	protected SearchContentProvider getContentAndLabelProvider(final boolean flat) {
		return new SearchContentProvider(this, flat);
	}
	
	@Override
	protected void configureTableViewer(final TableViewer tableViewer) {
		final SearchContentProvider contentAndLabelProvider = getContentAndLabelProvider(true);
		tableViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(contentAndLabelProvider));
		tableViewer.setContentProvider(contentAndLabelProvider);
		tableViewer.setComparator(contentAndLabelProvider.getComparator());
	}

	@Override
	protected void configureTreeViewer(final TreeViewer treeViewer) {
		final SearchContentProvider contentAndLabelProvider = getContentAndLabelProvider(false);
		treeViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(contentAndLabelProvider));
		treeViewer.setContentProvider(contentAndLabelProvider);
		treeViewer.setComparator(contentAndLabelProvider.getComparator());
	}

	@Override
	protected void elementsChanged(final Object[] elements) {
		getViewer().refresh();
	}
	
	@Override
	protected void showMatch(final Match match, final int currentOffset, final int currentLength, final boolean activate) throws PartInitException {
		final SearchMatch clonkMatch = (SearchMatch) match;
		StructureTextEditor editor;
		editor = (StructureTextEditor) StructureTextEditor.openDeclaration(clonkMatch.structure(), activate);
		editor.selectAndReveal(currentOffset, currentLength);
	}

	@Override
	public ShowInContext getShowInContext() {
		return new ShowInContext(null, null) {
			@Override
			public Object getInput() {
				final IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();
				final Object firstElm = selection.getFirstElement();
				if (firstElm instanceof Match)
					return ((Structure)((Match)firstElm).getElement()).resource();
				return selection.getFirstElement();
			}
		};
	}

	private static final String[] SHOW_IN_TARGETS = new String[] {
		IPageLayout.ID_PROJECT_EXPLORER
	};
	
	@Override
	public String[] getShowInTargetIds() {
		return SHOW_IN_TARGETS;
	}
	
	@Override
	protected void handleOpen(final OpenEvent event) {
		final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		if (selection.getFirstElement() instanceof Match) {
			final Match m = (Match) selection.getFirstElement();
			try {
				showMatch(m, m.getOffset(), m.getLength(), true);
			} catch (final PartInitException e) {
				e.printStackTrace();
			}
		}
		else
			super.handleOpen(event);
	}
	
	@Override
	public int getDisplayedMatchCount(final Object element) {
		return element instanceof Match ? 1 : 0;
	}
	
	@Override
	public Match[] getDisplayedMatches(final Object element) {
		return element instanceof Match ? new Match[] {(Match)element} : new Match[0];
	}

}
