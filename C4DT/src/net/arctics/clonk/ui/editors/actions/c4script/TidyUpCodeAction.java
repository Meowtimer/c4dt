package net.arctics.clonk.ui.editors.actions.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import net.arctics.clonk.index.IHasSubDeclarations;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.MutableRegion;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AppendableBackedExprWriter;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.Conf;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ExprWriter;
import net.arctics.clonk.parser.c4script.ast.PropListExpression;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction.CommandId;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;

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

@CommandId(id="ui.editors.actions.TidyUpCode")
public class TidyUpCodeAction extends ClonkTextEditorAction {

	public TidyUpCodeAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
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
		runOnDocument(editor.script(), selection, parser, document);
	}

	private static ExprElm codeFor(Declaration d) {
		if (d instanceof Variable) {
			ExprElm init = ((Variable)d).initializationExpression();
			if (init != null && init.owningDeclaration() != null && init.owningDeclaration() != d)
				return null;
			else
				return init;
		}
		else if (d instanceof Function)
			return ((Function)d).body();
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
		MutableRegion region = new MutableRegion(noSelection ? 0 : selection.getOffset(), noSelection ? document.getLength() : selection.getLength());
		synchronized (document) {
			TextChange textChange = new DocumentChange(Messages.TidyUpCodeAction_TidyUpCode, document);
			textChange.setEdit(new MultiTextEdit());
			List<Declaration> decs = new ArrayList<Declaration>();
			for (Declaration d : script.accessibleDeclarations(IHasSubDeclarations.VARIABLES|+IHasSubDeclarations.FUNCTIONS))
				if (!(d instanceof Variable && d.parentDeclaration() instanceof Function) && codeFor(d) != null)
					decs.add(d);
			Collections.sort(decs, new Comparator<Declaration>() {
				@Override
				public int compare(Declaration arg0, Declaration arg1) {
					ExprElm codeA = codeFor(arg0);
					ExprElm codeB = codeFor(arg1);
					return codeB.start()-codeA.start();
				}
			});
			for (Declaration d : decs)
				try {
					Function func = as(d, Function.class);
					// variable declared inside function -> ignore
					/*if (var != null && parser.getContainer().funcAt(var.getLocation().getOffset()) != null)
						continue;*/
					ExprElm elms = codeFor(d);
					if (func != null) {
						StringBuilder blockStringBuilder = new StringBuilder(region.getLength());
						switch (Conf.braceStyle) {
						case NewLine:
							blockStringBuilder.append('\n');
							break;
						case SameLine:
							blockStringBuilder.append(' ');
							break;
						}
						ExprElm original = elms;
						elms = new Block(elms.subElements());
						elms.setExprRegion(original);
						parser.setCurrentFunction(func);
						elms.exhaustiveOptimize(parser).print(blockStringBuilder, 0);
						String blockString = blockStringBuilder.toString();
						int blockBegin;
						int blockLength;
						// eat braces if new style func
						if (func.isOldStyle()) {
							blockBegin = elms.start();
							blockLength = elms.end() - blockBegin;
							blockBegin += func.bodyLocation().start();
						}
						else {
							blockBegin  = func.bodyLocation().start()-1;
							blockLength = func.bodyLocation().end()+1 - blockBegin;
						}
						// eat indentation
						while (blockBegin-1 >= func.header().end() && superflousBetweenFuncHeaderAndBody(document.getChar(blockBegin-1))) {
							blockBegin--;
							blockLength++;
						}
						try {
							textChange.addEdit(new ReplaceEdit(blockBegin, blockLength, blockString));
							// convert old style function to new style function
							String newHeader = func.headerString(false);
							textChange.addEdit(new ReplaceEdit(func.header().start(), func.header().getLength(), newHeader));
						} catch (MalformedTreeException mt) {
							System.out.println("Adding edit for " + func.name() + " failed");
						}
					}
					else
						replaceExpression(document, elms, parser, textChange);
				} catch (CloneNotSupportedException e1) {
					e1.printStackTrace();
				} catch (BadLocationException e) {
					e.printStackTrace();
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
		int oldStart = e.start();
		int oldLength = e.end()-e.start();
		while (superflousBetweenFuncHeaderAndBody(document.getChar(oldStart-1))) {
			oldStart--;
			oldLength++;
		}
		String oldString = document.get(oldStart, oldLength);
		ExprWriter newStringWriter = new AppendableBackedExprWriter(new StringBuilder());
		if (e instanceof PropListExpression)
			Conf.blockPrelude(newStringWriter, 0);
		e.exhaustiveOptimize(parser).print(newStringWriter, 0);
		String newString = newStringWriter.toString();
		if (!oldString.equals(newString)) try {
			textChange.addEdit(new ReplaceEdit(oldStart, oldLength, newString));
		} catch (MalformedTreeException malformed) {
			//malformed.printStackTrace();
			throw malformed;
		}
	}
	
}