package net.arctics.clonk.ui.editors.actions;

import java.util.LinkedList;

import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.parser.ExprTree.ExprElm;
import net.arctics.clonk.ui.editors.C4ScriptEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class ConvertOldCodeToNewCodeAction implements IEditorActionDelegate {

	private C4ScriptEditor editor;

	public void setActiveEditor(IAction action, IEditorPart editorPart) {
		editor = (C4ScriptEditor)editorPart;
	}

	public void run(IAction action) {
		ITextSelection selection = (ITextSelection)editor.getSelectionProvider().getSelection();
		final LinkedList<ExprElm> expressions = new LinkedList<ExprElm>();
		try {
			editor.reparseWithDocumentContents(new C4ScriptParser.IExpressionNotifiee() {
				public void parsedToplevelExpression(ExprElm expression) {
					expressions.addFirst(expression);
				}
			});
		} catch (CompilerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for (ExprElm e : expressions) {
			System.out.println(e.toString());
		}
	}

	public void selectionChanged(IAction arg0, ISelection arg1) {
		// blub
	}
	
}