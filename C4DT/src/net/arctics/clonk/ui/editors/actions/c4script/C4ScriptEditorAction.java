package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.ResourceBundle;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.ui.editors.c4script.DeclarationLocator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

public abstract class C4ScriptEditorAction extends TextEditorAction {

	public C4ScriptEditorAction(ResourceBundle bundle, String prefix, ITextEditor editor, String actionDefinitionId) {
		super(bundle, prefix, editor);
	}
	
	protected ExprElm getExprElmAtSelection() {
		ITextSelection selection = (ITextSelection) getTextEditor().getSelectionProvider().getSelection();
		IRegion r = new Region(selection.getOffset(), selection.getLength());
		try {
			return new DeclarationLocator(
				getTextEditor(),
				getTextEditor().getDocumentProvider().getDocument(getTextEditor().getEditorInput()),
				r
			).getExprAtRegion();
		} catch (BadLocationException e) {
			e.printStackTrace();
			return null;
		} catch (ParsingException e) {
			return null;
		}
	}
	
	protected Declaration getDeclarationAtSelection(boolean fallbackToCurrentFunction) {
		ITextSelection selection = (ITextSelection) getTextEditor().getSelectionProvider().getSelection();
		IRegion r = new Region(selection.getOffset(), selection.getLength());
		try {
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
		} catch (BadLocationException e) {
			e.printStackTrace();
			return null;
		} catch (ParsingException e) {
			return null;
		}
	}

}