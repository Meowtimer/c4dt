package net.arctics.clonk.ui.editors.actions;

import java.util.ResourceBundle;

import net.arctics.clonk.ui.editors.C4ScriptEditor;
import net.arctics.clonk.ui.editors.ClonkCommandIds;
import net.arctics.clonk.ui.editors.IdentInfo;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

public class OpenDeclarationAction extends TextEditorAction {

	public OpenDeclarationAction(ResourceBundle bundle, String prefix,
			ITextEditor editor) {
		super(bundle, prefix, editor);
		this.setActionDefinitionId(ClonkCommandIds.OPEN_DECLARATION);
	}

	@Override
	public void run() {
		ITextSelection selection = (ITextSelection) getTextEditor().getSelectionProvider().getSelection();
		IRegion r = new Region(selection.getOffset(), selection.getLength());
		try {
			IdentInfo info = new IdentInfo(
					getTextEditor(),
					getTextEditor().getDocumentProvider().getDocument(getTextEditor().getEditorInput()),
					r);
			if (info.getField() != null)
				C4ScriptEditor.openDeclaration(info.getField());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
