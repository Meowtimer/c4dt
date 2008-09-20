package net.arctics.clonk.ui.editors;

import org.eclipse.swt.graphics.Image;

import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.Utilities;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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
			C4Object obj = Utilities.getObjectForEditor(editor);
			if (obj != null) {
				TreeViewer treeViewer = this.getTreeViewer();
				if (treeViewer != null) {
					ClonkContentOutlineLabelAndContentProvider provider = new ClonkContentOutlineLabelAndContentProvider();
					treeViewer.setLabelProvider(provider);
					treeViewer.setContentProvider(provider);
					treeViewer.setInput(obj);
					treeViewer.setSorter(new ViewerSorter() {
						public int category(Object element) {
							return ((C4Field)element).sortCategory();
						}
					});
					treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
						public void selectionChanged(SelectionChangedEvent event) {
							if (event.getSelection().isEmpty()) {
								return;
							} else if (event.getSelection() instanceof IStructuredSelection) {
								editor.selectAndReveal(((C4Field)((IStructuredSelection)event.getSelection()).getFirstElement()).getLocation());
							}
						}
					});
				}
			}
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
		getTreeViewer().refresh();
	}

	
}
