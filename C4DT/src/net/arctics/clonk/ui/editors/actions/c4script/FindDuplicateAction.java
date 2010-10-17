package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.ResourceBundle;

import net.arctics.clonk.ui.editors.IClonkCommandIds;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

public class FindDuplicateAction extends TextEditorAction {
	public FindDuplicateAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
		this.setActionDefinitionId(IClonkCommandIds.FIND_DUPLICATES);
	}
	@Override
	public void run() {
		System.out.println("ugh");
	}
}
