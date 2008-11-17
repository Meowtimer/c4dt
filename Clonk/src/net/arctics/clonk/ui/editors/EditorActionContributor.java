package net.arctics.clonk.ui.editors;

import java.util.ResourceBundle;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

public class EditorActionContributor extends BasicTextEditorActionContributor {

	private RetargetTextEditorAction fContentAssist;
	//	
	public EditorActionContributor() {
		ResourceBundle messagesBundle = ResourceBundle.getBundle("net.arctics.clonk.ui.editors.Messages");
		fContentAssist = new RetargetTextEditorAction(messagesBundle, null);
		fContentAssist
				.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
//		fConvertOldCodeToNewCode = new RetargetTextEditorAction(messagesBundle, null);
//		fConvertOldCodeToNewCode.setActionDefinitionId(ClonkActionDefinitionIds.CONVERT_OLD_CODE_TO_NEW_CODE);
		// fIndexClonkDir = new
		// RetargetTextEditorAction(ResourceBundle.getBundle
		// ("net.arctics.clonk.ui.editors.Messages"),"IndexClonkDir.");
		// fIndexClonkDir.setActionDefinitionId(C4ScriptEditor.
		// ACTION_INDEX_CLONK_DIR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.texteditor.BasicTextEditorActionContributor#setActiveEditor
	 * (org.eclipse.ui.IEditorPart)
	 */
	@Override
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		// if (getActionBars().getToolBarManager().find(C4ScriptEditor.
		// ACTION_INDEX_CLONK_DIR) == null)
		// getActionBars().getToolBarManager().add(getAction((ITextEditor)part,
		// C4ScriptEditor.ACTION_INDEX_CLONK_DIR));
		fContentAssist.setAction(getAction((ITextEditor) part,
				ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS));
//		fConvertOldCodeToNewCode.setAction(getAction((ITextEditor)part, ClonkActionDefinitionIds.CONVERT_OLD_CODE_TO_NEW_CODE));
		// fIndexClonkDir.setAction(getAction((ITextEditor)part,
		// C4ScriptEditor.ACTION_INDEX_CLONK_DIR));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.texteditor.BasicTextEditorActionContributor#contributeToMenu
	 * (org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	public void contributeToMenu(IMenuManager menu) {
		// super.contributeToMenu(menu);
		if (fContentAssist != null) {
			IMenuManager editMenu = menu
					.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
			editMenu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS,
					fContentAssist);
//			editMenu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, blub);
		}
		// if (fIndexClonkDir != null) {
		// IMenuManager projectMenu=
		// menu.findMenuUsingPath(IWorkbenchActionConstants.M_PROJECT);
		// projectMenu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS,
		// fIndexClonkDir);
		// }
	}

}
