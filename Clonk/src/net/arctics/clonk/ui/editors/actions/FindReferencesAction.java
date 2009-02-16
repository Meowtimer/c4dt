package net.arctics.clonk.ui.editors.actions;

import java.util.ResourceBundle;

import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.ClonkCommandIds;
import net.arctics.clonk.ui.search.ClonkSearchQuery;

import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
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
				if (nature == null) {
					MessageBox mb = new MessageBox(getTextEditor().getSite().getShell(), SWT.OK | SWT.ICON_ERROR);
					mb.setMessage("Find References only works with files in your project (not external objects)");
					mb.open();
					return;
				}
				NewSearchUI.runQueryInBackground(new ClonkSearchQuery(field, nature));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
