package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.*;
import net.arctics.clonk.ui.editors.IClonkCommandIds;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.util.Pair;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

public class ConvertOldCodeToNewCodeAction extends TextEditorAction {

	public final static class FunctionStatements extends Pair<C4Function, LinkedList<Statement>> {
		public FunctionStatements(C4Function first, LinkedList<Statement> second) {
			super(first, second);
		}
	}
	
	public ConvertOldCodeToNewCodeAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
		this.setId(IClonkCommandIds.CONVERT_OLD_CODE_TO_NEW_CODE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		final C4ScriptEditor editor = (C4ScriptEditor)this.getTextEditor();
		final ITextSelection selection = (ITextSelection)editor.getSelectionProvider().getSelection();
		final IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		final LinkedList<FunctionStatements> statements = new LinkedList<FunctionStatements>();
		final int selLength = selection.getLength() == document.getLength() ? 0 : selection.getLength();
		C4ScriptParser parser;
		try {
			parser = editor.reparseWithDocumentContents(expressionCollector(selection, statements, selLength), false);
			// add functions that contain now statements and are therefore not collected
			Outer: for (C4Function f : parser.getContainer().functions()) {
				for (FunctionStatements s : statements)
					if (s.getFirst() == f)
						continue Outer;
				statements.add(new FunctionStatements(f, new LinkedList<Statement>()));
			}
		} catch (Exception e) {
			parser = null;
			e.printStackTrace();
		}
		runOnDocument(parser, selection, document, statements);
	}

	public static IExpressionListener expressionCollector(
			final ITextSelection selection,
			final LinkedList<FunctionStatements> statements,
			final int selLength) {
		return new IExpressionListener() {
			
			// don't immediately add comments for old-style funcs so that unrelated comments following a function don't get mangled in
			private List<Comment> commentsOnOld = new LinkedList<Comment>();
			
			public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
				if (!(expression instanceof Statement))
					return TraversalContinuation.Continue; // odd
				if (statements.size() == 0 || parser.getActiveFunc() != statements.getFirst().getFirst()) {
					statements.addFirst(new FunctionStatements(parser.getActiveFunc(), new LinkedList<Statement>()));
					commentsOnOld.clear();
				}
				if (selLength == 0 || (expression.getExprStart() >= selection.getOffset() && expression.getExprEnd() <= selection.getOffset()+selection.getLength())) {
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
			final LinkedList<FunctionStatements> statements
	) {
		synchronized (document) {
			final int selLength = selection.getLength() == document.getLength() ? 0 : selection.getLength();
			IDocumentExtension4 ext4 = null; // (document instanceof IDocumentExtension4) ? (IDocumentExtension4)document : null;
			DocumentRewriteSession session = ext4 != null ? ext4.startRewriteSession(DocumentRewriteSessionType.UNRESTRICTED) : null;
			TextChange textChange = new DocumentChange("Tidy Up Code", document);
			textChange.setEdit(new MultiTextEdit());
			for (FunctionStatements pair : statements) {
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
						StringBuilder blockStringBuilder = new StringBuilder(func.getBody().getLength());
						switch (C4ScriptExprTree.BraceStyle) {
						case NewLine:
							blockStringBuilder.append('\n');
							break;
						case SameLine:
							// noop
							break;
						}
						b.exhaustiveNewStyleReplacement(parser).print(blockStringBuilder, 1);
						String blockString = blockStringBuilder.toString();
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
						while (blockBegin-1 >= func.getHeader().getEnd() && superflousBetweenFuncHeaderAndBody(document.getChar(blockBegin-1))) {
							blockBegin--;
							blockLength++;
						}
						textChange.addEdit(new ReplaceEdit(blockBegin, blockLength, blockString));
						// convert old style function to new style function
						String newHeader = func.getHeaderString(false);
						textChange.addEdit(new ReplaceEdit(func.getHeader().getStart(), func.getHeader().getLength(), newHeader));
					}
					else {
						for (ExprElm e : elms) {
							replaceExpression(document, e, parser, textChange);
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
			try {
				textChange.perform(new NullProgressMonitor());
			} catch (CoreException e) {
				e.printStackTrace();
			}
			if (ext4 != null)
				ext4.stopRewriteSession(session);
		}
	}

	private static boolean superflousBetweenFuncHeaderAndBody(char c) {
		return c == '\t' || c == ' ' || c == '\n' || c == '\r';
	}
	
	private static void replaceExpression(IDocument document, ExprElm e, C4ScriptParser parser, TextChange textChange) throws BadLocationException, CloneNotSupportedException {
		String oldString = document.get(e.getExprStart(), e.getExprEnd()-e.getExprStart());
		String newString = e.exhaustiveNewStyleReplacement(parser).toString(2);
		if (!oldString.equals(newString))
			textChange.addEdit(new ReplaceEdit(e.getExprStart(), e.getExprEnd()-e.getExprStart(), newString));
	}
	
}