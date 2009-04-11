package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.*;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.ui.editors.c4script.ClonkCommandIds;
import net.arctics.clonk.util.Pair;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
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
		final LinkedList<Pair<C4Function, LinkedList<Statement>>> statements = new LinkedList<Pair<C4Function, LinkedList<Statement>>>();
		final int selLength = selection.getLength() == document.getLength() ? 0 : selection.getLength();
		C4ScriptParser parser;
		try {
			parser = editor.reparseWithDocumentContents(expressionCollector(selection, statements, selLength), false);
		} catch (Exception e) {
			parser = null;
			e.printStackTrace();
		}
		runOnDocument(parser, selection, document, statements);
	}

	public static IExpressionListener expressionCollector(
			final ITextSelection selection,
			final LinkedList<Pair<C4Function, LinkedList<Statement>>> statements,
			final int selLength) {
		return new IExpressionListener() {
			
			// don't immediately add comments for old-style funcs so that unrelated comments following a function don't get mangled in
			private List<Comment> commentsOnOld = new LinkedList<Comment>();
			
			public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
				if (!(expression instanceof Statement))
					return TraversalContinuation.Continue; // odd
				if (statements.size() == 0 || parser.getActiveFunc() != statements.getFirst().getFirst()) {
					statements.addFirst(new Pair<C4Function, LinkedList<Statement>>(parser.getActiveFunc(), new LinkedList<Statement>()));
					commentsOnOld.clear();
				}
				if (selLength == 0 || (expression.getExprStart() >= selection.getOffset() && expression.getExprEnd() < selection.getOffset()+selection.getLength())) {
					if (parser.getActiveFunc().isOldStyle()) {
						if (expression instanceof Comment)
							commentsOnOld.add((Comment)expression);
						else {
							// another statement follows comments -> add all comments to function
							for (Comment c : commentsOnOld)
								statements.getFirst().getSecond().addFirst(c);
							commentsOnOld.clear();
							statements.getFirst().getSecond().addFirst((Statement)expression);
						}
					} else {
						statements.getFirst().getSecond().addFirst((Statement)expression);
					}
				}
				return TraversalContinuation.Continue;
			}
		};
	}

	public static void runOnDocument(
		final C4ScriptParser parser,
		final ITextSelection selection,
		final IDocument document,
		final LinkedList<Pair<C4Function, LinkedList<Statement>>> statements
	) {
		final int selLength = selection.getLength() == document.getLength() ? 0 : selection.getLength();
		IDocumentExtension4 ext4 = (document instanceof IDocumentExtension4) ? (IDocumentExtension4)document : null;
		DocumentRewriteSession session = ext4 != null ? ext4.startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL) : null;
		for (Pair<C4Function, LinkedList<Statement>> pair : statements) {
			try {
				C4Function func = pair.getFirst();
				LinkedList<Statement> elms = pair.getSecond();
				boolean wholeFuncConversion = selLength == 0;
				parser.setActiveFunc(func);
				if (wholeFuncConversion) {
					Statement[] statementsInRightOrder = new Statement[elms.size()];
					int counter = statementsInRightOrder.length-1;
					for (Statement s : elms) {
						statementsInRightOrder[counter--] = s;
					}
					Block b = new Block(statementsInRightOrder);
					String blockString = b.exhaustiveNewStyleReplacement(parser).toString(1);
					int blockBegin;
					int blockLength;
					// eat braces if new style func
					if (func.isOldStyle()) {
						blockBegin = statementsInRightOrder[0].getExprStart();
						blockLength = statementsInRightOrder[elms.size()-1].getExprEnd() - blockBegin;
					}
					else {
						blockBegin  = func.getBody().getStart()-1;
						blockLength = func.getBody().getEnd()+1 - blockBegin;
					}
					// eat indentation
					while (blockBegin-1 > func.getHeader().getEnd() && isIndent(document.getChar(blockBegin-1))) {
						blockBegin--;
						blockLength++;
					}
					document.replace(blockBegin, blockLength, blockString);
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
		if (ext4 != null)
			ext4.stopRewriteSession(session);
	}

	private static boolean isIndent(char c) {
		return c == '\t' || c == ' ';
	}
	
	private static void replaceExpression(IDocument document, ExprElm e, C4ScriptParser parser) throws BadLocationException, CloneNotSupportedException {
		String oldString = document.get(e.getExprStart(), e.getExprEnd()-e.getExprStart());
		String newString = e.exhaustiveNewStyleReplacement(parser).toString(2);
		if (!oldString.equals(newString))
			document.replace(e.getExprStart(), e.getExprEnd()-e.getExprStart(), newString);
	}
	
}