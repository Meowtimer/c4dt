package net.arctics.clonk.ui.search;

import java.io.IOException;

import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.ui.editors.C4ScriptEditor;

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

public class ClonkSearchResultPage extends AbstractTextSearchViewPage implements IShowInSource, IShowInTargetList {
	
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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ShowInContext getShowInContext() {
		return new ShowInContext(null, null) {
			@Override
			public Object getInput() {
				TreeViewer treeViewer = (TreeViewer) getViewer();
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
				Object firstElm = selection.getFirstElement();
				if (firstElm instanceof Match) {
					return ((C4ScriptBase)((Match)firstElm).getElement()).getScriptFile();
				}
				return selection.getFirstElement();
			}
		};
	}

	private static final String[] SHOW_IN_TARGETS = new String[] {
		IPageLayout.ID_RES_NAV,
		"org.eclipse.ui.navigator.ProjectExplorer"
	};
	
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

}
