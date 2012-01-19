package net.arctics.clonk.ui.editors.actions.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.IHasSubDeclarations;
import net.arctics.clonk.parser.c4script.MutableRegion;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.Conf;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.ui.editors.ClonkCommandIds;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

public class TidyUpCodeAction extends TextEditorAction {

	public final static class CodeChunk {
		public Declaration relatedDeclaration;
		public List<ExprElm> expressions;
		// encompasses all expressions of the declaration
		public boolean complete;
		public CodeChunk(Declaration declaration, List<ExprElm> expressions) {
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
		C4ScriptParser parser;
		try {
			parser = editor.reparseWithDocumentContents(null, false);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		runOnDocument(editor.scriptBeingEdited(), selection, parser, document);
	}

	private static ExprElm codeFor(Declaration d) {
		if (d instanceof Variable)
			return ((Variable)d).getInitializationExpression();
		else if (d instanceof Function)
			return ((Function)d).getCodeBlock();
		else
			return null;
	}
	
	public static void runOnDocument(
		Script script,
		ITextSelection selection,
		C4ScriptParser parser,
		final IDocument document
	) {
		boolean noSelection = selection == null || selection.getLength() == 0 || selection.getLength() == document.getLength();
		MutableRegion region = new MutableRegion(0, 0);
		synchronized (document) {
			TextChange textChange = new DocumentChange(Messages.TidyUpCodeAction_TidyUpCode, document);
			textChange.setEdit(new MultiTextEdit());
			List<Declaration> decs = new ArrayList<Declaration>();
			for (Declaration d : script.allSubDeclarations(IHasSubDeclarations.VARIABLES|+IHasSubDeclarations.FUNCTIONS)) {
				if (!(d instanceof Variable && d.getParentDeclaration() instanceof Function) && codeFor(d) != null)
					decs.add(d);
			}
			Collections.sort(decs, new Comparator<Declaration>() {
				@Override
				public int compare(Declaration arg0, Declaration arg1) {
					ExprElm codeA = codeFor(arg0);
					ExprElm codeB = codeFor(arg1);
					return codeB.getExprStart()-codeA.getExprStart();
				}
			});
			for (Declaration d : decs) {
				try {
					Function func = as(d, Function.class);
					// variable declared inside function -> ignore
					/*if (var != null && parser.getContainer().funcAt(var.getLocation().getOffset()) != null)
						continue;*/
					ExprElm elms = codeFor(d);
					if (elms == null)
						continue;
					if (func != null && noSelection) {
						StringBuilder blockStringBuilder = new StringBuilder(region.getLength());
						switch (Conf.braceStyle) {
						case NewLine:
							blockStringBuilder.append('\n');
							break;
						case SameLine:
							// noop
							break;
						}
						elms = new Block(elms.subElements());
						elms.exhaustiveOptimize(parser).print(blockStringBuilder, 1);
						String blockString = blockStringBuilder.toString();
						int blockBegin;
						int blockLength;
						// eat braces if new style func
						if (func.isOldStyle()) {
							blockBegin = elms.getExprStart();
							blockLength = elms.getExprEnd() - blockBegin;
							blockBegin += func.getBody().getStart();
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
						try {
							textChange.addEdit(new ReplaceEdit(blockBegin, blockLength, blockString));
							// convert old style function to new style function
							String newHeader = func.getHeaderString(false);
							textChange.addEdit(new ReplaceEdit(func.getHeader().getStart(), func.getHeader().getLength(), newHeader));
						} catch (MalformedTreeException mt) {
							System.out.println("Adding edit for " + func.name() + " failed");
						}
					}
					else if (noSelection) {
						region.setStartAndEnd(
							selection.getOffset()-(func != null ? func.getBody().getOffset() : 0),
							selection.getOffset()-(func != null ? func.getBody().getOffset() : 0)+selection.getLength()
						);
						if (elms instanceof Block) {
							for (ExprElm e : elms.subElements()) {
								if (Utilities.regionContainsOtherRegion(region, e))
									replaceExpression(document, e, parser, textChange);
							}
						}
						else
							replaceExpression(document, elms, parser, textChange);
					}
				} catch (CloneNotSupportedException e1) {
					e1.printStackTrace();
				} catch (BadLocationException e) {
					e.printStackTrace();
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