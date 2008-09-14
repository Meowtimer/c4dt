package net.arctics.clonk.ui.editors;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;

public class EditorActionContributer extends BasicTextEditorActionContributor {


	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.BasicTextEditorActionContributor#setActiveEditor(org.eclipse.ui.IEditorPart)
	 */
	@Override
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		if (getActionBars().getToolBarManager().find(C4ScriptEditor.ACTION_INDEX_CLONK_DIR) == null)
			getActionBars().getToolBarManager().add(getAction((ITextEditor)part, C4ScriptEditor.ACTION_INDEX_CLONK_DIR));
	}

	
	
}
