package net.arctics.clonk.ui.editors.actions;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import net.arctics.clonk.parser.C4Function;
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
		final Map<C4Function, LinkedList<ExprElm>> expressions = new HashMap<C4Function, LinkedList<ExprElm>>();
		final int selLength = selection.getLength();
		C4ScriptParser parser;
		try {
			parser = editor.reparseWithDocumentContents(new IExpressionListener() {
				public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
					if (selLength == 0 || (expression.getExprStart() >= selection.getOffset() && expression.getExprEnd() < selection.getOffset()+selection.getLength())) {
						LinkedList<ExprElm> elms = expressions.get(parser.getActiveFunc());
						if (elms == null) {
							elms = new LinkedList<ExprElm>();
							expressions.put(parser.getActiveFunc(), elms);
						}
						elms.addFirst(expression);
					}
					return TraversalContinuation.Continue;
				}
			},false);
		} catch (CompilerException e1) {
			parser = null;
			e1.printStackTrace();
		}
		for (C4Function func : expressions.keySet()) {
			try {
				LinkedList<ExprElm> elms = expressions.get(func);
				for (ExprElm e : elms) {
					parser.setActiveFunc(func);
					String oldString = document.get(e.getExprStart(), e.getExprEnd()-e.getExprStart());
					String newString = e.exhaustiveNewStyleReplacement(parser).toString();
					if (!oldString.equals(newString))
						document.replace(e.getExprStart(), e.getExprEnd()-e.getExprStart(), newString);
				}
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