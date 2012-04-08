package net.arctics.clonk.ui.editors.c4script;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import net.arctics.clonk.Core;
import net.arctics.clonk.ui.editors.ClonkCommandIds;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

public class EditorActionContributor extends BasicTextEditorActionContributor {
	
	private static final ResourceBundle c4ScriptEditorMessagesBundle = ResourceBundle.getBundle(Core.id("ui.editors.c4script.actionsBundle")); //$NON-NLS-1$
	private static final ResourceBundle editorMessagesBundle = ResourceBundle.getBundle(Core.id("ui.editors.actionsBundle")); //$NON-NLS-1$

	private final List<RetargetTextEditorAction> actions = new ArrayList<RetargetTextEditorAction>();
	
	private RetargetTextEditorAction add(ResourceBundle bundle, String prefix, String id) {
		RetargetTextEditorAction a = new RetargetTextEditorAction(bundle, prefix);
		a.setActionDefinitionId(id);
		actions.add(a);
		return a;
	}
	
	public EditorActionContributor() {
		add(c4ScriptEditorMessagesBundle, null, ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		add(editorMessagesBundle, "OpenDeclaration.", ClonkCommandIds.OPEN_DECLARATION);
		add(c4ScriptEditorMessagesBundle, "TidyUpCode.", ClonkCommandIds.CONVERT_OLD_CODE_TO_NEW_CODE);
		add(c4ScriptEditorMessagesBundle, "FindReferences.", ClonkCommandIds.FIND_REFERENCES);
		add(c4ScriptEditorMessagesBundle, "RenameDeclaration.", ClonkCommandIds.RENAME_DECLARATION);
		add(c4ScriptEditorMessagesBundle, "FindDuplicates.", ClonkCommandIds.FIND_DUPLICATES);
	}

	@Override
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		for (RetargetTextEditorAction action : actions) {
			if (action != null) {
				if (part instanceof ITextEditor)
					action.setAction(getAction((ITextEditor) part, action.getActionDefinitionId()));
			}
		}
	}

	@Override
	public void contributeToMenu(IMenuManager menu) {
		super.contributeToMenu(menu);
		for (RetargetTextEditorAction action : actions) {
			if (action != null) {
				IMenuManager editMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
				editMenu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, action);
			}
		}
	}

}
