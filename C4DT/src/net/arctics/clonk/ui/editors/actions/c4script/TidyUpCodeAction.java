package net.arctics.clonk.ui.editors.actions.c4script;


import java.util.ResourceBundle;

import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction.CommandId;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
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
		converter().runOnDocument(editor.script(), selection, parser, document);
	}

	public static CodeConverter converter() {
		return new CodeConverter() {
			@Override
			protected ExprElm performConversion(C4ScriptParser parser, ExprElm expression) {
				try {
					return expression.exhaustiveOptimize(parser);
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
					return expression;
				}
			}
		};
	}

}