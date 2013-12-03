package net.arctics.clonk.ui.editors.actions.c4script;

import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction.CommandId;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.ui.search.DuplicatesSearchQuery;

import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.texteditor.ITextEditor;

@CommandId(id="ui.editors.actions.FindDuplicates")
public class FindDuplicatesAction extends ClonkTextEditorAction {
	public FindDuplicatesAction(final ResourceBundle bundle, final String prefix, final ITextEditor editor) {
		super(bundle, prefix, editor);
	}
	@Override
	public void run() {
		try {
			//getTextEditor().doSave(null);
			final C4ScriptEditor ed = (C4ScriptEditor) getTextEditor();
			ed.reparse(false);
			// force refreshing index so the functions acting as origins will be properly added to the declaration map
			ed.script().index().refresh(false);
			
			final List<Function> functions = new LinkedList<Function>();
			final Declaration declaration = declarationAtSelection(true);
			if (declaration instanceof Function)
				functions.add((Function) declaration);
			else for (final Function f : ((C4ScriptEditor)getTextEditor()).script().functions())
				functions.add(f);
			if (functions.size() > 0)
				NewSearchUI.runQueryInBackground(DuplicatesSearchQuery.queryWithFunctions(functions));
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
