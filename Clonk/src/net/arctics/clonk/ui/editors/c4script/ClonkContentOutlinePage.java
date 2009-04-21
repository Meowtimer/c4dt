package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.ui.navigator.ClonkOutlineProvider;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public class ClonkContentOutlinePage extends ContentOutlinePage {

	private C4ScriptEditor editor;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.contentoutline.ContentOutlinePage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		if (editor != null) {
			C4ScriptBase script = Utilities.getScriptForEditor(editor);
			if (script != null) {
				setTreeViewerInput(script);
			}
		}
	}

	private void setTreeViewerInput(C4ScriptBase obj) {
		TreeViewer treeViewer = this.getTreeViewer();
		if (treeViewer == null)
			return;
		ClonkOutlineProvider provider = new ClonkOutlineProvider();
		treeViewer.setLabelProvider(provider);
		treeViewer.setContentProvider(provider);
		treeViewer.setInput(obj);
		treeViewer.setSorter(new ViewerSorter() {
			@Override
			public int category(Object element) {
				return ((C4Field)element).sortCategory();
			}
		});
		treeViewer.refresh();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (event.getSelection().isEmpty()) {
			return;
		} else if (event.getSelection() instanceof IStructuredSelection) {
			editor.selectAndReveal(((C4Field)((IStructuredSelection)event.getSelection()).getFirstElement()).getLocation());
		}
	}

	/**
	 * @param editor the editor to set
	 */
	public void setEditor(C4ScriptEditor editor) {
		this.editor = editor;
	}

	/**
	 * @return the editor
	 */
	public C4ScriptEditor getEditor() {
		return editor;
	}

	public void refresh() {
		if (getTreeViewer().getInput() == null)
			setTreeViewerInput(Utilities.getScriptForEditor(editor));
		else
			getTreeViewer().refresh();
	}
	
	public void select(C4Field field) {
		TreeViewer viewer = getTreeViewer();
		viewer.removeSelectionChangedListener(this);
		try {
			this.setSelection(new StructuredSelection(field));
		} finally {
			viewer.addSelectionChangedListener(this);
		}
	}
	
}
