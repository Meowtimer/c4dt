package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.ResourceBundle;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.IClonkCommandIds;
import net.arctics.clonk.ui.search.ClonkSearchQuery;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class FindReferencesAction extends OpenDeclarationAction {

	public FindReferencesAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
		this.setActionDefinitionId(IClonkCommandIds.FIND_REFERENCES);
	}
	
	@Override
	public void run() {
		try {
			C4Declaration field = getDeclarationAtSelection();
			if (field != null) {
				ClonkProjectNature nature = Utilities.getClonkNature(field.getScript());				
				if (nature == null) {
					nature = Utilities.getClonkNature(getTextEditor()); 
				}
				if (nature == null) {
					MessageDialog.openError(getTextEditor().getSite().getShell(), "Find References", "Find References only works with files in your project (not external objects)");
					return;
				}
				NewSearchUI.runQueryInBackground(new ClonkSearchQuery(field, nature));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
