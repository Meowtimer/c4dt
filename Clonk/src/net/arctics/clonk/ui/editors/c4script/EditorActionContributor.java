package net.arctics.clonk.ui.editors.c4script;

import java.util.ResourceBundle;

import net.arctics.clonk.ClonkCore;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

public class EditorActionContributor extends BasicTextEditorActionContributor {

	private RetargetTextEditorAction fContentAssist;

	public EditorActionContributor() {
		ResourceBundle messagesBundle = ResourceBundle.getBundle(ClonkCore.id("ui.editors.c4script.messages")); //$NON-NLS-1$
		fContentAssist = new RetargetTextEditorAction(messagesBundle, null);
		fContentAssist.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
	}

	@Override
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		if (fContentAssist != null) {
			if (part instanceof ITextEditor)
				fContentAssist.setAction(getAction((ITextEditor) part, ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS));
		}
	}

	@Override
	public void contributeToMenu(IMenuManager menu) {
		super.contributeToMenu(menu);
		if (fContentAssist != null) {
			IMenuManager editMenu = menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
			editMenu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fContentAssist);
		}
	}

}
