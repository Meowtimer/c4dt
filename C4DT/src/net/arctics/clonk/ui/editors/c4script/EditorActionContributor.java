package net.arctics.clonk.ui.editors.c4script;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction;
import net.arctics.clonk.ui.editors.actions.OpenDeclarationAction;
import net.arctics.clonk.ui.editors.actions.c4script.FindDuplicatesAction;
import net.arctics.clonk.ui.editors.actions.c4script.FindReferencesAction;
import net.arctics.clonk.ui.editors.actions.c4script.RenameDeclarationAction;
import net.arctics.clonk.ui.editors.actions.c4script.TidyUpCodeAction;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

public class EditorActionContributor extends BasicTextEditorActionContributor {
	
	private final List<RetargetTextEditorAction> actions = new ArrayList<RetargetTextEditorAction>();
	
	private void add(ResourceBundle bundle, @SuppressWarnings("unchecked") Class<? extends ClonkTextEditorAction>... classes) {
		for (Class<? extends ClonkTextEditorAction> c : classes) {
			String prefix = ClonkTextEditorAction.resourceBundlePrefix(c);
			String id = ClonkTextEditorAction.idString(c);
			add(bundle, prefix, id);
		}
	}

	private void add(ResourceBundle bundle, String prefix, String id) {
		RetargetTextEditorAction a = new RetargetTextEditorAction(bundle, prefix);
		a.setActionDefinitionId(id);
		actions.add(a);
	}
	
	@SuppressWarnings("unchecked")
	public EditorActionContributor() {
		add(C4ScriptEditor.MESSAGES_BUNDLE, null, ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		add(ClonkTextEditor.MESSAGES_BUNDLE, OpenDeclarationAction.class);
		add(C4ScriptEditor.MESSAGES_BUNDLE, TidyUpCodeAction.class, FindReferencesAction.class, RenameDeclarationAction.class, FindDuplicatesAction.class);
	}

	@Override
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		for (RetargetTextEditorAction action : actions)
			if (action != null)
				if (part instanceof ITextEditor)
					action.setAction(getAction((ITextEditor) part, action.getActionDefinitionId()));
	}

	@Override
	public void contributeToMenu(IMenuManager menu) {
		super.contributeToMenu(menu);
		for (RetargetTextEditorAction action : actions)
			if (action != null) {
				IMenuManager editMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
				editMenu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, action);
			}
	}

}
