package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.ResourceBundle;

import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction.CommandId;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

@CommandId(id="ui.editors.actions.ToggleComment")
public class ToggleCommentAction extends ClonkTextEditorAction {

	public ToggleCommentAction(final ResourceBundle bundle, final String prefix, final ITextEditor editor) {
		super(bundle, prefix, editor);
	}
	
	@Override
	public void run() {
		final ITextSelection sel = (ITextSelection)getTextEditor().getSelectionProvider().getSelection();
		try {
			final IDocument document = getTextEditor().getDocumentProvider().getDocument(getTextEditor().getEditorInput());
			final IRegion line = document.getLineInformationOfOffset(sel.getOffset());
			final boolean isCommented = line.getLength() >= 2 && document.get(line.getOffset(), 2).equals("//");
			if (isCommented)
				document.replace(line.getOffset(), 2, "");
			else
				document.replace(line.getOffset(), 0, "//");
		} catch (final BadLocationException e) {
			e.printStackTrace();
		}
	}
	
}
