/**
 * 
 */
package net.arctics.clonk.ui.editors.ini;

import java.util.ResourceBundle;

import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.c4script.ColorManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

public class IniTextEditor extends ClonkTextEditor {
	
	public static final String PAGE_ID = "rawIniEditor";
	
	private String title;
	
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		super.doSave(progressMonitor);
	}
	
	public IniTextEditor() {
		super();
		setSourceViewerConfiguration(new IniSourceViewerConfiguration(new ColorManager(), this));
	}

	public void resetPartName() {
		setPartName(title);
	}
	
	@Override
	protected void createActions() {
		super.createActions();
		
		ResourceBundle messagesBundle = ResourceBundle.getBundle("net.arctics.clonk.ui.editors.ini.Messages"); //$NON-NLS-1$
		
		IAction action;
		action = new ContentAssistAction(messagesBundle,"IniContentAssist.",this); //$NON-NLS-1$
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, action);
	}
	
}