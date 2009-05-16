package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.ResourceBundle;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ParsingException;
import net.arctics.clonk.ui.editors.ClonkCommandIds;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.ui.editors.c4script.DeclarationLocator;

import org.eclipse.jface.text.BadLocationException;
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
	
	protected C4Declaration getDeclarationAtSelection() throws BadLocationException, ParsingException {
		ITextSelection selection = (ITextSelection) getTextEditor().getSelectionProvider().getSelection();
		IRegion r = new Region(selection.getOffset(), selection.getLength());
		DeclarationLocator info = new DeclarationLocator(
			getTextEditor(),
			getTextEditor().getDocumentProvider().getDocument(getTextEditor().getEditorInput()),
			r
		);
		return info.getDeclaration();
	}

	@Override
	public void run() {
		try {
			C4Declaration field = getDeclarationAtSelection();
			if (field != null)
				C4ScriptEditor.openDeclaration(field);
		} catch (Exception e) {
			// so what
		}
	}
}
