package net.arctics.clonk.ui.editors.actions;

import java.util.LinkedList;

import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.parser.ExprTree.ExprElm;
import net.arctics.clonk.ui.editors.C4ScriptEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class ConvertOldCodeToNewCodeHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		C4ScriptEditor editor = (C4ScriptEditor)HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getActiveEditor();
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
		return null;
	}
	
}