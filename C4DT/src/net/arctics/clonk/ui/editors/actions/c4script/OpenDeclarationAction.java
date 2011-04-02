package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.ResourceBundle;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.ui.editors.IClonkCommandIds;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.ui.editors.c4script.DeclarationLocator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

public class OpenDeclarationAction extends TextEditorAction {

	public OpenDeclarationAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
		this.setActionDefinitionId(IClonkCommandIds.OPEN_DECLARATION);
	}

	protected Declaration getDeclarationAtSelection(boolean fallbackToCurrentFunction) throws BadLocationException, ParsingException {
		ITextSelection selection = (ITextSelection) getTextEditor().getSelectionProvider().getSelection();
		IRegion r = new Region(selection.getOffset(), selection.getLength());
		DeclarationLocator info = new DeclarationLocator(
			getTextEditor(),
			getTextEditor().getDocumentProvider().getDocument(getTextEditor().getEditorInput()),
			r
		);
		if (info.getDeclaration() != null)
			return info.getDeclaration();
		else if (fallbackToCurrentFunction && getTextEditor() instanceof C4ScriptEditor)
			return ((C4ScriptEditor)getTextEditor()).getFuncAtCursor();
		else
			return null;
	}

	@Override
	public void run() {
		// OpenDeclarationAction is for all text editors in the plugin so it opens declarations by querying for hyperlinks instead of relying on a script being edited
		IHyperlink hyperlink = ((ClonkTextEditor)getTextEditor()).getHyperlinkAtCurrentSelection();
		if (hyperlink != null)
			hyperlink.open();
	}
}
