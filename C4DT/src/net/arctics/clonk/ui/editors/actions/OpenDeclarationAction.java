package net.arctics.clonk.ui.editors.actions;

import java.util.ResourceBundle;

import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction.CommandId;

import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;

@CommandId(id="ui.editors.actions.OpenDeclaration")
public class OpenDeclarationAction extends ClonkTextEditorAction {

	public OpenDeclarationAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
	}

	@Override
	public void run() {
		// OpenDeclarationAction is for all text editors in the plugin so it opens declarations by querying for hyperlinks instead of relying on a script being edited
		IHyperlink hyperlink = ((ClonkTextEditor)getTextEditor()).hyperlinkAtCurrentSelection();
		if (hyperlink != null)
			hyperlink.open();
	}
}
