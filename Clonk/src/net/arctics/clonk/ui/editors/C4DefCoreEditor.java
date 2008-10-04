package net.arctics.clonk.ui.editors;

import java.util.ResourceBundle;

import net.arctics.clonk.parser.C4DefCoreWrapper;
import net.arctics.clonk.ui.editors.actions.IndexClonkDir;
import net.arctics.clonk.ui.navigator.ClonkLabelProvider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;

public class C4DefCoreEditor extends TextEditor {

	public C4DefCoreEditor() {
		super();
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.editors.text.TextEditor#createActions()
	 */
	@Override
	protected void createActions() {
		super.createActions();
		IAction action = new IndexClonkDir(ResourceBundle.getBundle("net.arctics.clonk.ui.editors.Messages"),"IndexClonkDir.",this); 
		action.setToolTipText("Index Clonk directory");
		action.setActionDefinitionId(C4ScriptEditor.ACTION_INDEX_CLONK_DIR);
		action.setDisabledImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD_DISABLED));
		action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_LCL_LINKTO_HELP));
		setAction(C4ScriptEditor.ACTION_INDEX_CLONK_DIR, action);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#editorSaved()
	 */
//	@Override
//	protected void editorSaved() {
//		super.editorSaved();
//		try {
//			C4DefCoreParser.getInstance().update(getEditingFile());
//		} catch (CoreException e) {
//			e.printStackTrace();
//		}
//		ClonkLabelProvider.instance.testRefresh();
//		
//	}
	
	protected IFile getEditingFile() {
		if (getEditorInput() instanceof FileEditorInput) {
			return ((FileEditorInput)getEditorInput()).getFile();
		}
		else return null;
	}
	
	

}
