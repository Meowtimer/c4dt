package net.arctics.clonk.ui.editors.actions;

import java.util.LinkedList;
import java.util.ResourceBundle;

import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.parser.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.ui.editors.C4ScriptEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

public class ConvertOldCodeToNewCodeAction extends TextEditorAction {

	public ConvertOldCodeToNewCodeAction(ResourceBundle bundle,
			String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		super.run();
		System.out.println("dies ding hier wird ausgeführt");
	}

	public void run(IAction action) {
		System.out.println("und das hier natürlich nicht");
		C4ScriptEditor editor = (C4ScriptEditor)this.getTextEditor();
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
	
}