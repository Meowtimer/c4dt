package net.arctics.clonk.ui.editors.actions;

import java.util.ResourceBundle;

import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;

import net.arctics.clonk.ui.editors.StructureTextEditor;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction.CommandId;

@CommandId(id="ui.editors.actions.OpenDeclaration")
public class OpenDeclarationAction extends ClonkTextEditorAction {
	public OpenDeclarationAction(final ResourceBundle bundle, final String prefix, final ITextEditor editor) {
		super(bundle, prefix, editor);
	}
	@Override
	public void run() {
		// OpenDeclarationAction is for all text editors in the plugin so it opens declarations by querying for hyperlinks instead of relying on a script being edited
		final IHyperlink hyperlink = ((StructureTextEditor)getTextEditor()).hyperlinkAtCurrentSelection();
		if (hyperlink != null) {
			hyperlink.open();
		}
	}
}
