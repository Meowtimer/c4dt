package net.arctics.clonk.ui.editors.actions;

import java.util.ResourceBundle;

import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.ClonkCommandIds;
import net.arctics.clonk.ui.search.ClonkSearchQuery;

import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class FindReferencesAction extends OpenDeclarationAction {

	public FindReferencesAction(ResourceBundle bundle, String prefix,
			ITextEditor editor) {
		super(bundle, prefix, editor);
		this.setActionDefinitionId(ClonkCommandIds.FIND_REFERENCES);
	}
	
	@Override
	public void run() {
		try {
			C4Field field = getFieldAtSelection();
			if (field != null) {
				ClonkProjectNature nature = Utilities.getProject(field.getScript());				
				if (nature == null) {
					nature = Utilities.getProject(getTextEditor()); 
				}
				if (nature == null)
					return; // FIXME
				NewSearchUI.runQueryInBackground(new ClonkSearchQuery(field));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
