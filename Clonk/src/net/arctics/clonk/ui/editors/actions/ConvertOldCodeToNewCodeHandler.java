package net.arctics.clonk.ui.editors.actions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedList;

import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.parser.C4ScriptParser.ExprElm;
import net.arctics.clonk.ui.editors.C4ScriptEditor;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

public class ConvertOldCodeToNewCodeHandler implements IHandler {

	public void addHandlerListener(IHandlerListener handlerListener) {
	}

	public void dispose() {
		// TODO Auto-generated method stub
		
	}

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

	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isHandled() {
		// TODO Auto-generated method stub
		return false;
	}

	public void removeHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub
		
	}
	
}