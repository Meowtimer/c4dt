package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.ResourceBundle;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.ClonkCommandIds;
import net.arctics.clonk.ui.search.ReferencesQuery;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class FindReferencesAction extends C4ScriptEditorAction {

	public FindReferencesAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor, ClonkCommandIds.FIND_REFERENCES);
	}
	
	@Override
	public void run() {
		try {
			Declaration declaration = getDeclarationAtSelection(false);
			if (declaration != null) {
				ClonkProjectNature nature = ClonkProjectNature.get(declaration.script());				
				if (nature == null) {
					nature = ClonkProjectNature.get(getTextEditor()); 
				}
				if (nature == null) {
					MessageDialog.openError(getTextEditor().getSite().getShell(), Messages.FindReferencesAction_Label, Messages.FindReferencesAction_OnlyWorksWithinProject);
					return;
				}
				NewSearchUI.runQueryInBackground(new ReferencesQuery(declaration, nature));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
