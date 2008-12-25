package net.arctics.clonk.ui.search;

import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.ui.editors.C4ScriptEditor;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.PartInitException;

public class ClonkSearchResultPage extends AbstractTextSearchViewPage {
	
	@Override
	protected void clear() {
		// yep
	}

	@Override
	protected void configureTableViewer(TableViewer tableViewer) {
		// don't care
	}

	@Override
	protected void configureTreeViewer(TreeViewer treeViewer) {
		 treeViewer.setLabelProvider(new ClonkSearchLabelProvider());
		 treeViewer.setContentProvider(new ClonkSearchContentProvider(this));
	}

	@Override
	protected void elementsChanged(Object[] elements) {
		getViewer().refresh();
	}
	
	@Override
	protected void showMatch(Match match, int currentOffset, int currentLength,
			boolean activate) throws PartInitException {
		ClonkSearchMatch clonkMatch = (ClonkSearchMatch) match;
		try {
			C4ScriptEditor editor = (C4ScriptEditor) C4ScriptEditor.openDeclaration(clonkMatch.getScript(), activate);
			editor.selectAndReveal(currentOffset, currentLength);
		} catch (CompilerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
