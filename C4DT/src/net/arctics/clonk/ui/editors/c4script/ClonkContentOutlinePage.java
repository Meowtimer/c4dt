package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.parser.Declaration;
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
			Declaration topLevelDeclaration = getEditor().topLevelDeclaration();
			if (topLevelDeclaration != null) {
				setTreeViewerInput(topLevelDeclaration);
			}
		}
	}

	private static final ViewerSorter DECLARATION_SORTER = new ViewerSorter() {
		@Override
		public int category(Object element) {
			return ((Declaration)element).sortCategory();
		}
	};
	
	private void setTreeViewerInput(Declaration obj) {
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
			Declaration dec = (Declaration)((IStructuredSelection)event.getSelection()).getFirstElement();
			dec = dec.latestVersion();
			if (dec != null) {
				editor.selectAndReveal(dec.getLocation());
			}
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
		Declaration newInput = getEditor().topLevelDeclaration();
		if (getTreeViewer().getInput() != newInput)
			setTreeViewerInput(newInput);
		else
			getTreeViewer().refresh();
	}
	
	public void select(Declaration field) {
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
