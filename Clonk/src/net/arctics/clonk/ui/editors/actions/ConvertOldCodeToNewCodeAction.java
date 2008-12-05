package net.arctics.clonk.ui.editors.actions;

import java.util.LinkedList;
import java.util.ResourceBundle;

import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.parser.Pair;
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
	
	private void replaceExpression(IDocument document, ExprElm e, C4ScriptParser parser) throws BadLocationException, CloneNotSupportedException {
		String oldString = document.get(e.getExprStart(), e.getExprEnd()-e.getExprStart());
		String newString = e.exhaustiveNewStyleReplacement(parser).toString(2);
		if (!oldString.equals(newString))
			document.replace(e.getExprStart(), e.getExprEnd()-e.getExprStart(), newString);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		final C4ScriptEditor editor = (C4ScriptEditor)this.getTextEditor();
		final ITextSelection selection = (ITextSelection)editor.getSelectionProvider().getSelection();
		final IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		final LinkedList<Pair<C4Function, LinkedList<Statement>>> statements = new LinkedList<Pair<C4Function, LinkedList<Statement>>>();
		final int selLength = selection.getLength() == document.getLength() ? 0 : selection.getLength();
		C4ScriptParser parser;
		try {
			parser = editor.reparseWithDocumentContents(new IExpressionListener() {
				public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
					if (!(expression instanceof Statement))
						return TraversalContinuation.Continue; // odd
					if (statements.size() == 0 || parser.getActiveFunc() != statements.getFirst().getFirst())
						statements.addFirst(new Pair<C4Function, LinkedList<Statement>>(parser.getActiveFunc(), new LinkedList<Statement>()));
					if (selLength == 0 || (expression.getExprStart() >= selection.getOffset() && expression.getExprEnd() < selection.getOffset()+selection.getLength())) {
						statements.getFirst().getSecond().addFirst((Statement)expression);
					}
					return TraversalContinuation.Continue;
				}
			},false);
		} catch (CompilerException e1) {
			parser = null;
			e1.printStackTrace();
		}
		for (Pair<C4Function, LinkedList<Statement>> pair : statements) {
			try {
				C4Function func = pair.getFirst();
				LinkedList<Statement> elms = pair.getSecond();
				boolean wholeFuncConversion = func.isOldStyle() && selLength == 0;
				parser.setActiveFunc(func);
				if (wholeFuncConversion) {
					Statement[] statementsInRightOrder = new Statement[elms.size()];
					int counter = statementsInRightOrder.length-1;
					for (Statement s : elms) {
						statementsInRightOrder[counter--] = s;
					}
					Block b = new Block(statementsInRightOrder);
					String blockString = b.exhaustiveNewStyleReplacement(parser).toString(1);
					int blockBegin = statementsInRightOrder[0].getExprStart();
					// eat indentation
					while (blockBegin-1 > func.getHeader().getEnd() && isIndent(document.getChar(blockBegin-1)))
						blockBegin--;
					int blockEnd   = statementsInRightOrder[elms.size()-1].getExprEnd() - blockBegin;
					document.replace(blockBegin, blockEnd, blockString);
					// convert old style function to new style function
					String newHeader = func.getHeaderString(false);
					document.replace(func.getHeader().getStart(), func.getHeader().getLength(), newHeader);
				}
				else {
					for (ExprElm e : elms) {
						replaceExpression(document, e, parser);
					}
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

	private boolean isIndent(char c) {
		return c == '\t' || c == ' ';
	}
	
}