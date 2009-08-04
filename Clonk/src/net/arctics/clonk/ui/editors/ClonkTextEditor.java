package net.arctics.clonk.ui.editors;

import java.util.ResourceBundle;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4Structure;
import net.arctics.clonk.ui.editors.actions.c4script.OpenDeclarationAction;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.ui.editors.c4script.ClonkContentOutlinePage;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class ClonkTextEditor extends TextEditor {
	
	protected ClonkContentOutlinePage outlinePage;
	private ShowInAdapter showInAdapter;
	
	public void selectAndReveal(IRegion location) {
		this.selectAndReveal(location.getOffset(), location.getLength());
	}
	
	public void refreshOutline() {
		if (outlinePage != null) // don't start lazy loading of outlinePage
			outlinePage.refresh();
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
		if (IShowInSource.class.equals(adapter) || IShowInTargetList.class.equals(adapter)) {
			if (showInAdapter == null)
				showInAdapter = new ShowInAdapter(this);
			return showInAdapter;
		}
		return super.getAdapter(adapter);
	}
	
	public static IEditorPart openDeclaration(C4Declaration target, boolean activate) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchPage workbenchPage = workbench.getActiveWorkbenchWindow().getActivePage();
		C4Structure structure = target.getTopLevelStructure();
		if (structure != null) {
			IEditorInput input = structure.getEditorInput();
			if (input != null) {
				try {
					IEditorDescriptor descriptor = input instanceof IFileEditorInput ? IDE.getEditorDescriptor(((IFileEditorInput)input).getFile()) : null;
					String editorId = descriptor != null ? descriptor.getId() : "clonk.editors.C4ScriptEditor"; 
					IEditorPart editor = IDE.openEditor(workbenchPage, input, editorId, activate);
					if (editor instanceof ClonkTextEditor) {
						ClonkTextEditor clonkTextEditor = (ClonkTextEditor) editor;
						if (target != structure) {
							if (structure.dirty() && clonkTextEditor instanceof C4ScriptEditor)
								try {
									((C4ScriptEditor) clonkTextEditor).reparseWithDocumentContents(null, false);
								} catch (Exception e) {
									e.printStackTrace();
								}
								target = target.latestVersion();
								if (target != null)
									clonkTextEditor.selectAndReveal(target.getRegionToSelect());
						}
					} else if (editor instanceof AbstractTextEditor) {
						AbstractTextEditor ed = (AbstractTextEditor) editor;
						ed.selectAndReveal(target.getLocation().getStart(), target.getLocation().getEnd()-target.getLocation().getStart());
					}
					return editor;
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	public static IEditorPart openDeclaration(C4Declaration target) {
		return openDeclaration(target, true);
	}
	
	/**
	 * Return the declaration that represents the file being edited
	 * @return the declaration
	 */
	public C4Declaration getTopLevelDeclaration() {
		return null;
	}
	
	@Override
	protected void createActions() {
		super.createActions();
		
		IAction action;
		
		ResourceBundle messagesBundle = ResourceBundle.getBundle(ClonkCore.id("ui.editors.Messages")); //$NON-NLS-1$
		action = new OpenDeclarationAction(messagesBundle,"OpenDeclaration.",this); //$NON-NLS-1$
		setAction(IClonkCommandIds.OPEN_DECLARATION, action);
		
		if (getSourceViewerConfiguration().getContentAssistant(getSourceViewer()) != null) {
			action = new ContentAssistAction(messagesBundle, "ContentAssist.", this); //$NON-NLS-1$
			action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
			setAction(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, action);
		}
	}
	
	@Override
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		addAction(menu, IClonkCommandIds.OPEN_DECLARATION);
	}
	
	public IHyperlink hyperlinkAtOffset(int offset) {
		IHyperlinkDetector[] detectors = getSourceViewerConfiguration().getHyperlinkDetectors(getSourceViewer());
		// emulate
		getSourceViewerConfiguration().getHyperlinkPresenter(getSourceViewer()).hideHyperlinks();
		IRegion r = new Region(offset, 0);
		for (IHyperlinkDetector d : detectors) {
			IHyperlink[] hyperlinks = d.detectHyperlinks(getSourceViewer(), r, false);
			if (hyperlinks != null && hyperlinks.length > 0)
				return hyperlinks[0];
		}
		return null;
	}
	
}
