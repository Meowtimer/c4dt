package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.ResourceBundle;

import net.arctics.clonk.ui.editors.ClonkCommandIds;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

public class ToggleCommentAction extends ClonkTextEditorAction {

	public ToggleCommentAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor, ClonkCommandIds.TOGGLE_COMMENT);
	}
	
	@Override
	public void run() {
		ITextSelection sel = (ITextSelection)getTextEditor().getSelectionProvider().getSelection();
		try {
			IDocument document = getTextEditor().getDocumentProvider().getDocument(getTextEditor().getEditorInput());
			IRegion line = document.getLineInformationOfOffset(sel.getOffset());
			boolean isCommented = line.getLength() >= 2 && document.get(line.getOffset(), 2).equals("//");
			if (isCommented)
				document.replace(line.getOffset(), 2, "");
			else
				document.replace(line.getOffset(), 0, "//");
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
	
}
