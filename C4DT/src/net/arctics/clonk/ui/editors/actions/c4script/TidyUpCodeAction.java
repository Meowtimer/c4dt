package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.Conf;
import net.arctics.clonk.parser.c4script.ast.Comment;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ScriptParserListener;
import net.arctics.clonk.parser.c4script.ast.IScriptParserListener;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.c4script.ast.TraversalContinuation;
import net.arctics.clonk.ui.editors.IClonkCommandIds;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.util.ArrayUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

public class TidyUpCodeAction extends TextEditorAction {

	public final static class CodeChunk {
		public C4Declaration relatedDeclaration;
		public List<ExprElm> expressions;
		// encompasses all expressions of the declaration
		public boolean complete;
		public CodeChunk(C4Declaration declaration, List<ExprElm> expressions) {
			super();
			this.relatedDeclaration = declaration;
			this.expressions = expressions;
		}
		@Override
		public String toString() {
			return (relatedDeclaration != null ? relatedDeclaration.toString() : "<No Declaration>") + " " + expressions.toString();
		}
	}
	
	public TidyUpCodeAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
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
		final LinkedList<CodeChunk> chunks = new LinkedList<CodeChunk>();
		final int selLength = selection.getLength() == document.getLength() ? 0 : selection.getLength();
		C4ScriptParser parser;
		try {
			parser = editor.reparseWithDocumentContents(expressionCollector(selection, chunks, selLength), false);
			// add functions that contain no statements (those won't get collected by the expression collector)
			Outer: for (C4Function f : parser.getContainer().functions()) {
				for (CodeChunk s : chunks)
					if (s.relatedDeclaration == f)
						continue Outer;
				chunks.add(new CodeChunk(f, new LinkedList<ExprElm>()));
			}
		} catch (Exception e) {
			parser = null;
			e.printStackTrace();
		}
		runOnDocument(parser, selection.getLength() == document.getLength() || selection.getLength() == 0, document, chunks);
	}

	public static IScriptParserListener expressionCollector(
			final ITextSelection selection,
			final LinkedList<CodeChunk> chunks,
			final int selLength) {
		return new ScriptParserListener() {
			
			// don't immediately add comments for old-style funcs so that unrelated comments following a function don't get mangled in
			private List<Comment> commentsOnOld = new LinkedList<Comment>();
			
			public TraversalContinuation expressionDetected(ExprElm expression, C4ScriptParser parser) {
				C4Function activeFunc = parser.getCurrentFunc();
				// initialization expression for variable for example... needs to be reformatted as well
				if (activeFunc == null) {
					chunks.addFirst(new CodeChunk(parser.getCurrentVariableBeingDeclared(), ArrayUtil.list(expression)));
					return TraversalContinuation.Continue;
				}
				if (!(expression instanceof Statement))
					return TraversalContinuation.Continue; // odd
				if (chunks.size() == 0 || activeFunc != chunks.getFirst().relatedDeclaration) {
					chunks.addFirst(new CodeChunk(activeFunc, new LinkedList<ExprElm>()));
					commentsOnOld.clear();
				}
				if (selLength == 0 || (expression.getExprStart() >= selection.getOffset() && expression.getExprEnd() <= selection.getOffset()+selection.getLength())) {
					if (activeFunc.isOldStyle()) {
						if (expression instanceof Comment)
							commentsOnOld.add((Comment)expression);
						else {
							// another statement follows comments -> add all comments to function
							for (Comment c : commentsOnOld)
								chunks.getFirst().expressions.add(0, c);
							commentsOnOld.clear();
							chunks.getFirst().expressions.add(0, expression);
						}
					} else {
						chunks.getFirst().expressions.add(0, expression);
					}
				}
				return TraversalContinuation.Continue;
			}
		};
	}

	public static void runOnDocument(
			final C4ScriptParser parser,
			boolean wholeFuncConversion,
			final IDocument document,
			final LinkedList<CodeChunk> chunks
	) {
		synchronized (document) {
			TextChange textChange = new DocumentChange(Messages.TidyUpCodeAction_TidyUpCode, document);
			textChange.setEdit(new MultiTextEdit());
			for (CodeChunk chunk : chunks) {
				try {
					C4Function func = chunk.relatedDeclaration instanceof C4Function ? (C4Function)chunk.relatedDeclaration : null;
					C4Variable var = chunk.relatedDeclaration instanceof C4Variable ? (C4Variable)chunk.relatedDeclaration : null;
					IRegion region = func != null ? func.getBody() : var.getInitializationExpressionLocation();
					List<ExprElm> elms = chunk.expressions;
					parser.setCurrentFunc(func);
					if (func != null && wholeFuncConversion) {
						ExprElm[] expressionsInRightOrder = new ExprElm[elms.size()];
						int counter = expressionsInRightOrder.length-1;
						for (ExprElm s : elms) {
							expressionsInRightOrder[counter--] = s;
						}
						Block b = new Block(expressionsInRightOrder);
						StringBuilder blockStringBuilder = new StringBuilder(region.getLength());
						switch (Conf.braceStyle) {
						case NewLine:
							blockStringBuilder.append('\n');
							break;
						case SameLine:
							// noop
							break;
						}
						b.exhaustiveOptimize(parser).print(blockStringBuilder, 1);
						String blockString = blockStringBuilder.toString();
						int blockBegin;
						int blockLength;
						// eat braces if new style func
						if (func.isOldStyle()) {
							blockBegin = expressionsInRightOrder[0].getExprStart();
							blockLength = expressionsInRightOrder[elms.size()-1].getExprEnd() - blockBegin;
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
					e1.printStackTrace();
				} catch (CloneNotSupportedException e1) {
					e1.printStackTrace();
				}
			}
			try {
				textChange.perform(new NullProgressMonitor());
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	private static boolean superflousBetweenFuncHeaderAndBody(char c) {
		return c == '\t' || c == ' ' || c == '\n' || c == '\r';
	}
	
	private static void replaceExpression(IDocument document, ExprElm e, C4ScriptParser parser, TextChange textChange) throws BadLocationException, CloneNotSupportedException {
		String oldString = document.get(e.getExprStart(), e.getExprEnd()-e.getExprStart());
		String newString = e.exhaustiveOptimize(parser).toString(2);
		if (!oldString.equals(newString))
			textChange.addEdit(new ReplaceEdit(e.getExprStart(), e.getExprEnd()-e.getExprStart(), newString));
	}
	
}