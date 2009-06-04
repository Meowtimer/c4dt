package net.arctics.clonk.ui.editors;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.ITextEditor;

public class ShowInAdapter implements IShowInSource, IShowInTargetList {

	private IEditorPart editor;
	
	public ShowInAdapter(IEditorPart editor) {
		super();
		this.editor = editor;
	}

	public ShowInContext getShowInContext() {
		return new ShowInContext(null, null) {
			@Override
			public Object getInput() {
				return editor.getEditorInput();
			}
			@Override
			public ISelection getSelection() {
				if (editor instanceof ITextEditor)
					return ((ITextEditor)editor).getSelectionProvider().getSelection();
				return null;
			}
		};
	}

	public String[] getShowInTargetIds() {
		return new String[] {
			IPageLayout.ID_RES_NAV,
			//IPageLayout.ID_PROJECT_EXPLORER -- Eclipse 3.5
			"org.eclipse.ui.navigator.ProjectExplorer" // FIXME: constant for this?
		};
	}

}
