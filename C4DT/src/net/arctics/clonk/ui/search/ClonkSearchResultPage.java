package net.arctics.clonk.ui.search;

import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.ui.editors.ClonkTextEditor;

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
import net.arctics.clonk.ui.search.ClonkSearchContentProvider;

public class ClonkSearchResultPage extends AbstractTextSearchViewPage implements IShowInSource, IShowInTargetList {
	
	@Override
	protected void clear() {
		// yep
	}

	protected ClonkSearchContentProvider getContentAndLabelProvider(boolean flat) {
		return new ClonkSearchContentProvider(this, flat);
	}
	
	@Override
	protected void configureTableViewer(TableViewer tableViewer) {
		ClonkSearchContentProvider contentAndLabelProvider = getContentAndLabelProvider(true);
		tableViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(contentAndLabelProvider));
		tableViewer.setContentProvider(contentAndLabelProvider);
		tableViewer.setComparator(contentAndLabelProvider.getComparator());
	}

	@Override
	protected void configureTreeViewer(TreeViewer treeViewer) {
		ClonkSearchContentProvider contentAndLabelProvider = getContentAndLabelProvider(false);
		treeViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(contentAndLabelProvider));
		treeViewer.setContentProvider(contentAndLabelProvider);
		treeViewer.setComparator(contentAndLabelProvider.getComparator());
	}

	@Override
	protected void elementsChanged(Object[] elements) {
		getViewer().refresh();
	}
	
	@Override
	protected void showMatch(Match match, int currentOffset, int currentLength, boolean activate) throws PartInitException {
		ClonkSearchMatch clonkMatch = (ClonkSearchMatch) match;
		ClonkTextEditor editor;
		editor = (ClonkTextEditor) ClonkTextEditor.openDeclaration(clonkMatch.getStructure(), activate);
		editor.selectAndReveal(currentOffset, currentLength);
	}

	@Override
	public ShowInContext getShowInContext() {
		return new ShowInContext(null, null) {
			@Override
			public Object getInput() {
				IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();
				Object firstElm = selection.getFirstElement();
				if (firstElm instanceof Match) {
					return ((Structure)((Match)firstElm).getElement()).resource();
				}
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
	protected void handleOpen(OpenEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		if (selection.getFirstElement() instanceof Match) {
			Match m = (Match) selection.getFirstElement();
			try {
				showMatch(m, m.getOffset(), m.getLength(), true);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		}
		else
			super.handleOpen(event);
	}
	
	@Override
	public int getDisplayedMatchCount(Object element) {
		return element instanceof Match ? 1 : 0;
	}
	
	@Override
	public Match[] getDisplayedMatches(Object element) {
		return element instanceof Match ? new Match[] {(Match)element} : new Match[0];
	}

}
