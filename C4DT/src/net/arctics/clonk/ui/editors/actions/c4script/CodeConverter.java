package net.arctics.clonk.ui.editors.actions.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.index.IHasSubDeclarations;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Conf;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AppendableBackedExprWriter;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.ExprWriter;
import net.arctics.clonk.parser.c4script.ast.PropListExpression;

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

public abstract class CodeConverter {

	private final Map<Declaration, ExprElm> codeMapping = new HashMap<Declaration, ExprElm>();
	
	private ExprElm codeFor(Declaration declaration) {
		ExprElm mapped = codeMapping.get(declaration);
		if (mapped == null) {
			if (declaration instanceof Variable) {
				ExprElm init = ((Variable)declaration).initializationExpression();
				if (init != null && init.owningDeclaration() != null && init.owningDeclaration() != declaration)
					return null;
				else
					mapped = init;
			}
			else if (declaration instanceof Function) {
				Function func = (Function)declaration;
				ExprElm body = func.body();
				Block block = new Block(body.subElements());
				block.setExprRegion(body);
				body = block;
				int blockBegin;
				int blockLength;
				// eat braces if new style func
				if (func.isOldStyle()) {
					blockBegin = body.start();
					blockLength = body.end() - blockBegin;
					blockBegin += func.bodyLocation().start();
				}
				else {
					blockBegin  = func.bodyLocation().start()-1;
					blockLength = func.bodyLocation().end()+1 - blockBegin;
				}
				body.setExprRegion(blockBegin, blockBegin+blockLength);
				mapped = body;
			}
			else
				return null;
			codeMapping.put(declaration, mapped);
		}
		return mapped;
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
					ExprElm elms = codeFor(d);
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

	protected abstract ExprElm performConversion(C4ScriptParser parser, ExprElm expression);

	private static boolean superflousBetweenFuncHeaderAndBody(char c) {
		return c == '\t' || c == ' ' || c == '\n' || c == '\r';
	}

	private void replaceExpression(Declaration d, IDocument document, ExprElm e, C4ScriptParser parser, TextChange textChange) throws BadLocationException, CloneNotSupportedException {
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
		parser.setCurrentFunction(as(d, Function.class));
		if (d instanceof Function)
			Conf.blockPrelude(newStringWriter, 0);
		performConversion(parser, e).print(newStringWriter, 0);
		parser.setCurrentFunction(null);
		String newString = newStringWriter.toString();
		if (!oldString.equals(newString)) try {
			textChange.addEdit(new ReplaceEdit(oldStart, oldLength, newString));
		} catch (MalformedTreeException malformed) {
			//malformed.printStackTrace();
			throw malformed;
		}
	}

}