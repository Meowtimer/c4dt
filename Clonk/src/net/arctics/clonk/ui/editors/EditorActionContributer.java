package net.arctics.clonk.ui.editors;

import java.util.ResourceBundle;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

public class EditorActionContributer extends BasicTextEditorActionContributor {

	private RetargetTextEditorAction fContentAssist;
	private RetargetTextEditorAction fIndexClonkDir;
//	
	public EditorActionContributer() {
		fContentAssist = new RetargetTextEditorAction(ResourceBundle.getBundle("net.arctics.clonk.ui.editors.Messages"),null);
		fContentAssist.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		fIndexClonkDir = new RetargetTextEditorAction(ResourceBundle.getBundle("net.arctics.clonk.ui.editors.Messages"),"IndexClonkDir.");
		fIndexClonkDir.setActionDefinitionId(C4ScriptEditor.ACTION_INDEX_CLONK_DIR);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.BasicTextEditorActionContributor#setActiveEditor(org.eclipse.ui.IEditorPart)
	 */
	@Override
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
//		if (getActionBars().getToolBarManager().find(C4ScriptEditor.ACTION_INDEX_CLONK_DIR) == null)
//			getActionBars().getToolBarManager().add(getAction((ITextEditor)part, C4ScriptEditor.ACTION_INDEX_CLONK_DIR));
		fContentAssist.setAction(getAction((ITextEditor) part, ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS));
		fIndexClonkDir.setAction(getAction((ITextEditor)part, C4ScriptEditor.ACTION_INDEX_CLONK_DIR));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.BasicTextEditorActionContributor#contributeToMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	public void contributeToMenu(IMenuManager menu) {
//		super.contributeToMenu(menu);
		if (fContentAssist != null) {
			IMenuManager editMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
			editMenu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fContentAssist);
		}
		if (fIndexClonkDir != null) {
			IMenuManager projectMenu= menu.findMenuUsingPath(IWorkbenchActionConstants.M_PROJECT);
			projectMenu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fIndexClonkDir);
		}
	}

	
}
