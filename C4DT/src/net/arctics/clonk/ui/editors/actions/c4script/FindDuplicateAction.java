package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.ui.editors.ClonkCommandIds;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.ui.search.FindDuplicatesQuery;

import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class FindDuplicateAction extends C4ScriptEditorAction {
	public FindDuplicateAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor, ClonkCommandIds.FIND_DUPLICATES);
	}
	@Override
	public void run() {
		try {
			//getTextEditor().doSave(null);
			C4ScriptEditor ed = (C4ScriptEditor) getTextEditor();
			ed.reparseWithDocumentContents(null, false);
			// force refreshing index so the functions acting as origins will be properly added to the declaration map
			ed.scriptBeingEdited().getIndex().refreshIndex();
			
			List<Function> functions = new LinkedList<Function>();
			Declaration declaration = getDeclarationAtSelection(true);
			if (declaration instanceof Function)
				functions.add((Function) declaration);
			else for (Function f : ((C4ScriptEditor)getTextEditor()).scriptBeingEdited().functions())
				functions.add(f);
			if (functions.size() > 0)
				NewSearchUI.runQueryInBackground(FindDuplicatesQuery.queryWithFunctions(functions));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
