package net.arctics.clonk.ui.editors.actions.c4script;

import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.arctics.clonk.index.IHasSubDeclarations;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.ASTNodePrinter;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Conf;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AppendableBackedExprWriter;
import net.arctics.clonk.parser.c4script.ast.Block;
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

	private final Map<Declaration, ASTNode> codeMapping = new HashMap<Declaration, ASTNode>();
	
	private ASTNode codeFor(Declaration declaration) {
		ASTNode mapped = codeMapping.get(declaration);
		if (mapped == null) {
			if (declaration instanceof Variable) {
				ASTNode init = ((Variable)declaration).initializationExpression();
				if (init != null && init.owningDeclaration() != null && init.owningDeclaration() != declaration)
					return null;
				else
					mapped = init;
			}
			else if (declaration instanceof Function) {
				Function func = (Function)declaration;
				ASTNode body = func.body();
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
					ASTNode codeA = codeFor(arg0);
					ASTNode codeB = codeFor(arg1);
					return codeB.start()-codeA.start();
				}
			});
			for (Declaration d : decs)
				try {
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
		int oldStart = e.start();
		int oldLength = e.end()-e.start();
		while (superflousBetweenFuncHeaderAndBody(document.getChar(oldStart-1))) {
			oldStart--;
			oldLength++;
		}
		String oldString = document.get(oldStart, oldLength);
		ASTNodePrinter newStringWriter = new AppendableBackedExprWriter(new StringBuilder());
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