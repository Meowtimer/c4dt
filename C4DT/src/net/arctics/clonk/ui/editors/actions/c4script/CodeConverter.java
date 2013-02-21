package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.arctics.clonk.index.IHasSubDeclarations;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Conf;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IHasCode;
import net.arctics.clonk.parser.c4script.InitializationFunction;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AppendableBackedExprWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

public abstract class CodeConverter {

	private ASTNode codeFor(Declaration declaration) {
		return declaration instanceof IHasCode ? ((IHasCode)declaration).code() : null;
	}

	public void runOnDocument(
		Script script,
		ITextSelection selection,
		C4ScriptParser parser,
		final IDocument document
	) {
		synchronized (document) {
			TextChange textChange = new DocumentChange(Messages.TidyUpCodeAction_TidyUpCode, document);
			textChange.setEdit(new MultiTextEdit());
			List<Declaration> decs = new ArrayList<Declaration>();
			for (Declaration d : script.accessibleDeclarations(IHasSubDeclarations.VARIABLES|IHasSubDeclarations.FUNCTIONS))
				if (!(d instanceof Variable && d.parentDeclaration() instanceof Function) && codeFor(d) != null)
					decs.add(d);
			Collections.sort(decs, new Comparator<Declaration>() {
				@Override
				public int compare(Declaration a, Declaration b) {
					ASTNode codeA = codeFor(a);
					ASTNode codeB = codeFor(b);
					return codeB.start()-codeA.start();
				}
			});
			for (Declaration d : decs)
				try {
					if (d instanceof InitializationFunction)
						continue;
					ASTNode elms = codeFor(d);
					if (d instanceof Function) {
						Function f = (Function)d;
						StringBuilder header = new StringBuilder(f.header().getLength()+10);
						f.printHeader(new AppendableBackedExprWriter(header), false);
						textChange.addEdit(new ReplaceEdit(f.header().start(), f.header().getLength(), header.toString()));
					}
					replaceExpression(d, document, elms, parser, textChange);
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

	protected abstract ASTNode performConversion(C4ScriptParser parser, ASTNode expression);

	private static boolean superflousBetweenFuncHeaderAndBody(char c) {
		return c == '\t' || c == ' ' || c == '\n' || c == '\r';
	}

	private void replaceExpression(Declaration d, IDocument document, ASTNode e, C4ScriptParser parser, TextChange textChange) throws BadLocationException, CloneNotSupportedException {
		final IRegion region = e.absolute(); 
		int oldStart = region.getOffset();
		int oldLength = region.getLength();
		while (superflousBetweenFuncHeaderAndBody(document.getChar(oldStart-1))) {
			oldStart--;
			oldLength++;
		}
		String oldString = document.get(oldStart, oldLength);
		StringBuilder builder = new StringBuilder();
		ASTNodePrinter newStringWriter = new AppendableBackedExprWriter(builder);
		if (d instanceof Function)
			Conf.blockPrelude(newStringWriter, 0);
		performConversion(parser, e).print(newStringWriter, 0);
		String newString = newStringWriter.toString();
		if (!oldString.equals(newString)) try {
			textChange.addEdit(new ReplaceEdit(oldStart, oldLength, newString));
		} catch (MalformedTreeException malformed) {
			throw malformed;
		}
	}

}