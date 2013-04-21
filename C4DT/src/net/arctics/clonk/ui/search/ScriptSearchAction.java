package net.arctics.clonk.ui.search;

import java.util.ResourceBundle;

import net.arctics.clonk.Core;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction.CommandId;

import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.texteditor.ITextEditor;

@CommandId(id="ui.editors.actions.C4ScriptSearch")
public class ScriptSearchAction extends ClonkTextEditorAction {
	public ScriptSearchAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
	}
	@Override
	public void run() {
		NewSearchUI.openSearchDialog(getTextEditor().getSite().getWorkbenchWindow(),
			Core.id("ui.C4ScriptSearchPage"));
	}
}