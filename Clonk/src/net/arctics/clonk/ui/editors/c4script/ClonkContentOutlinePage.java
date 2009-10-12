package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.navigator.ClonkOutlineProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public class ClonkContentOutlinePage extends ContentOutlinePage {

	private ClonkTextEditor editor;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.contentoutline.ContentOutlinePage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		if (editor != null) {
			C4Declaration topLevelDeclaration = getEditor().getTopLevelDeclaration();
			if (topLevelDeclaration != null) {
				setTreeViewerInput(topLevelDeclaration);
			}
		}
	}

	private static final ViewerSorter DECLARATION_SORTER = new ViewerSorter() {
		@Override
		public int category(Object element) {
			return ((C4Declaration)element).sortCategory();
		}
	};
	
	private void setTreeViewerInput(C4Declaration obj) {
		TreeViewer treeViewer = this.getTreeViewer();
		if (treeViewer == null)
			return;
		ClonkOutlineProvider provider = new ClonkOutlineProvider();
		treeViewer.setLabelProvider(provider);
		treeViewer.setContentProvider(provider);
		treeViewer.setSorter(DECLARATION_SORTER);
		treeViewer.setInput(obj);
		treeViewer.refresh();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (event.getSelection().isEmpty()) {
			return;
		} else if (event.getSelection() instanceof IStructuredSelection) {
			editor.selectAndReveal(((C4Declaration)((IStructuredSelection)event.getSelection()).getFirstElement()).getLocation());
		}
	}

	/**
	 * @param clonkTextEditor the editor to set
	 */
	public void setEditor(ClonkTextEditor clonkTextEditor) {
		this.editor = clonkTextEditor;
	}

	/**
	 * @return the editor
	 */
	public ClonkTextEditor getEditor() {
		return editor;
	}

	public void refresh() {
		C4Declaration newInput = getEditor().getTopLevelDeclaration();
		if (getTreeViewer().getInput() != newInput)
			setTreeViewerInput(newInput);
		else
			getTreeViewer().refresh();
	}
	
	public void select(C4Declaration field) {
		TreeViewer viewer = getTreeViewer();
		viewer.removeSelectionChangedListener(this);
		try {
			this.setSelection(new StructuredSelection(field));
		} finally {
			viewer.addSelectionChangedListener(this);
		}
	}
	
	public void setInput(Object input) {
		getTreeViewer().setInput(input);
	}

	public void clear() {
		getTreeViewer().setInput(null);
	}
	
}
