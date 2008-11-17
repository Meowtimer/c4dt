package net.arctics.clonk.ui.editors.actions;

import java.util.LinkedList;
import java.util.ResourceBundle;

import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.parser.C4ScriptExprTree.*;
import net.arctics.clonk.ui.editors.C4ScriptEditor;
import net.arctics.clonk.ui.editors.ClonkCommandIds;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

public class ConvertOldCodeToNewCodeAction extends TextEditorAction {

	public ConvertOldCodeToNewCodeAction(ResourceBundle bundle,
			String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
		this.setId(ClonkCommandIds.CONVERT_OLD_CODE_TO_NEW_CODE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		final C4ScriptEditor editor = (C4ScriptEditor)this.getTextEditor();
		final ITextSelection selection = (ITextSelection)editor.getSelectionProvider().getSelection();
		final IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		final LinkedList<ExprElm> expressions = new LinkedList<ExprElm>();
		C4ScriptParser parser;
		try {
			parser = editor.reparseWithDocumentContents(new IExpressionListener() {
				public TraversalContinuation expressionDetected(ExprElm expression) {
					if (selection == null || (expression.getExprStart() >= selection.getOffset() && expression.getExprEnd() < selection.getOffset()+selection.getLength())) {
						expressions.addFirst(expression);
					}
					return TraversalContinuation.Continue;
				}
			},false);
		} catch (CompilerException e1) {
			parser = null;
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for (ExprElm e : expressions) {
			try {
				document.replace(e.getExprStart(), e.getExprEnd()-e.getExprStart(), e.exhaustiveNewStyleReplacement(parser).toString());
			} catch (BadLocationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (CloneNotSupportedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
}