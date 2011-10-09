package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.ResourceBundle;

import net.arctics.clonk.ui.editors.ClonkCommandIds;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;

public class OpenDeclarationAction extends C4ScriptEditorAction {

	public OpenDeclarationAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor, ClonkCommandIds.OPEN_DECLARATION);
	}

	@Override
	public void run() {
		// OpenDeclarationAction is for all text editors in the plugin so it opens declarations by querying for hyperlinks instead of relying on a script being edited
		IHyperlink hyperlink = ((ClonkTextEditor)getTextEditor()).hyperlinkAtCurrentSelection();
		if (hyperlink != null)
			hyperlink.open();
	}
}
