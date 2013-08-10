package net.arctics.clonk.ui.editors.actions.c4script;


import java.util.ResourceBundle;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.Tidy;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction.CommandId;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;

import org.eclipse.jface.text.IDocument;
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
		//final ITextSelection selection = (ITextSelection)editor.getSelectionProvider().getSelection();
		final IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		try {
			editor.reparse(false);
		} catch (final Exception e) {
			e.printStackTrace();
			return;
		}
		converter().runOnDocument(editor.script(), document);
	}

	public static CodeConverter converter() {
		return new CodeConverter() {
			@Override
			protected ASTNode performConversion(ASTNode expression, Declaration declaration, ICodeConverterContext context) {
				try {
					return new Tidy(expression.parent(Script.class).strictLevel()).tidyExhaustive(expression);
				} catch (final CloneNotSupportedException e) {
					e.printStackTrace();
					return expression;
				}
			}
		};
	}

}