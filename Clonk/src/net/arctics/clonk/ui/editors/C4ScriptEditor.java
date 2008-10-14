package net.arctics.clonk.ui.editors;

import java.util.ResourceBundle;

import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.ui.editors.actions.IndexClonkDir;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class C4ScriptEditor extends AbstractDecoratedTextEditor {

	private ColorManager colorManager;
	private ClonkContentOutlinePage outlinePage;
	public static final String ACTION_INDEX_CLONK_DIR = "net.arctics.clonk.indexClonkCommand";
	
	public C4ScriptEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new ClonkSourceViewerConfiguration(colorManager,this));
		setDocumentProvider(new ClonkDocumentProvider(this));
	}

//	/* (non-Javadoc)
//	 * @see org.eclipse.ui.part.WorkbenchPart#getTitleImage()
//	 */
//	@Override
//	public Image getTitleImage() {
//		return ClonkLabelProvider.computeImage("c4script", "icons/c4scriptIcon.png",	Utilities.getEditingFile(this));
//	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#getPartName()
	 */
	public String getPartName() {
		// TODO Auto-generated method stub
//		String part = super.getPartName();
		IResource res = (IResource) getEditorInput().getAdapter(IResource.class);
		if (res == null) return super.getPartName();
		return res.getParent().getName() + "/" + super.getPartName();
	}
	
	public ClonkContentOutlinePage getOutlinePage() {
		if (outlinePage == null) {
			outlinePage = new ClonkContentOutlinePage();
			outlinePage.setEditor(this);
		}
		return outlinePage;
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		if (IContentOutlinePage.class.equals(adapter)) {
			return getOutlinePage();
		}
		return super.getAdapter(adapter);
	}

	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
	
	protected void createActions() {
		super.createActions();
		IAction action= new ContentAssistAction(ResourceBundle.getBundle("net.arctics.clonk.ui.editors.Messages"),"ClonkContentAssist.",this);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		
		setAction(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, action); //$NON-NLS-1$
		
//		markAsStateDependentAction(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, true); //$NON-NLS-1$
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(action, helpContextId);
		
		action = new ContentAssistAction(ResourceBundle.getBundle("net.arctics.clonk.ui.editors.Messages"),"ClonkContentAssist.",this);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SHOW_INFORMATION);
		setAction(ITextEditorActionDefinitionIds.SHOW_INFORMATION, action);
		
		action = new ContentAssistAction(ResourceBundle.getBundle("net.arctics.clonk.ui.editors.Messages"),"ClonkContentAssist.",this);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
		setAction(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION, action);
		
		action = new IndexClonkDir(ResourceBundle.getBundle("net.arctics.clonk.ui.editors.Messages"),"IndexClonkDir.",this); 
		action.setToolTipText("Index Clonk directory");
		action.setActionDefinitionId(ACTION_INDEX_CLONK_DIR);
		action.setDisabledImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD_DISABLED));
		action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_LCL_LINKTO_HELP));
		setAction(ACTION_INDEX_CLONK_DIR, action);
		
	}

	public void selectAndReveal(SourceLocation location) {
		this.selectAndReveal(location.getStart(), location.getEnd() - location.getStart());
	}

	@Override
	protected void doSetSelection(ISelection selection) {
		super.doSetSelection(selection);
		C4Object obj = Utilities.getObjectForEditor(this);
		if (obj != null) {
			C4Function func = obj.funcAt((ITextSelection)selection);
			outlinePage.select(func);
		}
	}

}
