package net.arctics.clonk.ui.editors;

import java.util.ResourceBundle;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.DeclarationLocation;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.ui.editors.actions.c4script.OpenDeclarationAction;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;
import net.arctics.clonk.ui.editors.c4script.ClonkContentAssistant;
import net.arctics.clonk.ui.editors.c4script.ClonkContentOutlinePage;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * Base class for Clonk text editors.
 * @author madeen
 *
 */
public class ClonkTextEditor extends TextEditor {
	
	protected ClonkContentOutlinePage outlinePage;
	private ShowInAdapter showInAdapter;
	
	/**
	 * Select and reveal some location in the text file.
	 * @param location
	 */
	public void selectAndReveal(IRegion location) {
		this.selectAndReveal(location.getOffset(), location.getLength());
	}
	
	public void selectAndRevealLine(int line) {
		IDocument d = getSourceViewer().getDocument();
		try {
			IRegion r = new Region(d.getLineOffset(line), 0);
			this.selectAndReveal(r);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Clear the outline.
	 */
	public void clearOutline() {
		if (outlinePage != null)
			outlinePage.clear();
	}
	
	/**
	 * Refresh the outline so the new contents of the {@link #topLevelDeclaration()} will be shown.
	 */
	public void refreshOutline() {
		if (outlinePage != null) // don't start lazy loading of outlinePage
			outlinePage.refresh();
	}
	
	/**
	 * Return the outline page of this text editor.
	 * @return
	 */
	public ClonkContentOutlinePage getOutlinePage() {
		if (outlinePage == null) {
			outlinePage = new ClonkContentOutlinePage();
			outlinePage.setEditor(this);
		}
		return outlinePage;
	}
	
	/**
	 * Handle adaptering for {@link IContentOutlinePage} and {@link IShowInSource}
	 */
	@Override
	@SuppressWarnings("rawtypes")
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
	
	/**
	 * Utility method to open some {@link Declaration} in some variant of ClonkTextEditor.
	 * @param target The {@link Declaration} to open
	 * @param activate Whether to activate the editor after opening it.
	 * @return The {@link IEditorPart}. Will most likely refer to a ClonkTextEditor object or be null due to some failure.
	 */
	public static IEditorPart openDeclaration(Declaration target, boolean activate) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchPage workbenchPage = workbench.getActiveWorkbenchWindow().getActivePage();
		Structure structure = target.topLevelStructure();
		if (structure instanceof IHasEditorRefWhichEnablesStreamlinedOpeningOfDeclarations) {
			IEditorPart ed = ((IHasEditorRefWhichEnablesStreamlinedOpeningOfDeclarations) structure).getEditor();
			revealInEditor(target, structure, ed);
			return ed;
		}
		if (structure != null) {
			IEditorInput input = structure.makeEditorInput();
			if (input != null) {
				try {
					IEditorDescriptor descriptor = input instanceof IFileEditorInput ? IDE.getEditorDescriptor(((IFileEditorInput)input).getFile()) : null;
					String editorId = descriptor != null ? descriptor.getId() : "clonk.editors.C4ScriptEditor";  //$NON-NLS-1$
					IEditorPart editor = IDE.openEditor(workbenchPage, input, editorId, activate);
					revealInEditor(target, structure, editor);
					return editor;
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
			// if a definition has no script fall back to opening DefCore.txt
			else if (structure instanceof Definition) {
				Definition obj = (Definition) structure;
				IFile defCore = obj.defCoreFile();
				if (defCore != null) {
					try {
						IDE.openEditor(workbenchPage, defCore);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * See {@link #openDeclaration(Declaration, boolean)}. The editor will always be revealed.
	 * @param target The {@link Declaration} to open.
	 * @return The opened {@link IEditorPart} or null due to failure.
	 */
	public static IEditorPart openDeclaration(Declaration target) {
		return openDeclaration(target, true);
	}

	/**
	 * Open a {@link DeclarationLocation}.
	 * @param location The location to open
	 * @param activate Whether to activate the editor after opening the location.
	 * @return
	 */
	public static IEditorPart openDeclarationLocation(DeclarationLocation location, boolean activate) {
		try {
			IEditorPart ed = null;
			if (location.resource() instanceof IFile) {
				IEditorDescriptor descriptor = IDE.getEditorDescriptor((IFile) location.resource());
				ed = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), (IFile) location.resource(), descriptor.getId());
			}
			else if (location.resource() instanceof IContainer) {
				Definition def = Definition.definitionCorrespondingToFolder((IContainer) location.resource());
				if (def != null) {
					ed = openDeclaration(def);
				}
			}
			if (ed instanceof ClonkTextEditor) {
				((ClonkTextEditor) ed).selectAndReveal(location.location());
			}
			return ed;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Reveal a {@link Declaration} in an editor.
	 * @param target The {@link Declaration} to reveal
	 * @param structure The {@link Structure} the {@link Declaration} is contained in.
	 * @param editor The editor to reveal the {@link Declaration} in. Special treatment for {@link ClonkTextEditor}, fallback for {@link AbstractTextEditor}.
	 */
	private static void revealInEditor(Declaration target, Structure structure, IEditorPart editor) {
		if (editor instanceof ClonkTextEditor) {
			ClonkTextEditor clonkTextEditor = (ClonkTextEditor) editor;
			if (target != structure && target.location() != null) {
				if (structure.isDirty() && clonkTextEditor instanceof C4ScriptEditor) {
					try {
						((C4ScriptEditor) clonkTextEditor).reparseWithDocumentContents(null, false);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				Declaration old = target;
				target = target.latestVersion();
				if (target == null)
					target = old;
				if (target != null)
					clonkTextEditor.selectAndReveal(target.regionToSelect());
			}
		} else if (editor instanceof AbstractTextEditor) {
			AbstractTextEditor ed = (AbstractTextEditor) editor;
			ed.selectAndReveal(target.location().getStart(), target.location().getEnd()-target.location().getStart());
		}
	}
	
	/**
	 * Return the declaration that represents the file being edited
	 * @return the declaration
	 */
	public Declaration topLevelDeclaration() {
		return null;
	}
	
	private static final ResourceBundle messagesBundle = ResourceBundle.getBundle(ClonkCore.id("ui.editors.actionsBundle")); //$NON-NLS-1$
	
	@Override
	protected void createActions() {
		super.createActions();
		
		IAction action;
		action = new OpenDeclarationAction(messagesBundle,"OpenDeclaration.",this); //$NON-NLS-1$
		setAction(ClonkCommandIds.OPEN_DECLARATION, action);
		
		if (getSourceViewerConfiguration().getContentAssistant(getSourceViewer()) != null) {
			action = new ContentAssistAction(messagesBundle, "ContentAssist.", this); //$NON-NLS-1$
			action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
			setAction(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, action);
		}
	}
	
	@Override
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		if (topLevelDeclaration() != null) {
			menu.add(new Separator(ClonkCommandIds.GROUP_CLONK));
			addAction(menu, ClonkCommandIds.OPEN_DECLARATION);
		}
	}
	
	/**
	 * Create a {@link IHyperlink} at the given offset in the text document using the same mechanism that is being used to create hyperlinks when ctrl-hovering.
	 * This hyperlink will be used for functionality like {@link OpenDeclarationAction} that will not directly operate on specific kinds of {@link Declaration}s and is thus dependent on the {@link ClonkTextEditor} class returning adequate hyperlinks. 
	 * @param offset The offset 
	 * @return
	 */
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
	
	/**
	 * Invoke {@link #hyperlinkAtOffset(int)} using the current selection offset.
	 * @return The hyperlink returned by {@link #hyperlinkAtOffset(int)}
	 */
	public IHyperlink hyperlinkAtCurrentSelection() {
	    ITextSelection selection = (ITextSelection) this.getSelectionProvider().getSelection();
		IHyperlink hyperlink = this.hyperlinkAtOffset(selection.getOffset());
		return hyperlink;
	}
	
	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		// set part name to reflect the folder the file is in
		IResource res = (IResource) getEditorInput().getAdapter(IResource.class);
		if (res != null && res.getParent() != null) {
			setPartName(res.getParent().getName() + "/" + res.getName()); //$NON-NLS-1$
		}
	}

	@Override
	protected void initializeEditor() {
		super.initializeEditor();
		setPreferenceStore(EditorsUI.getPreferenceStore());
	}
	
	public ClonkContentAssistant getContentAssistant() {
		return (ClonkContentAssistant) getSourceViewerConfiguration().getContentAssistant(getSourceViewer());
	}
	
	public void completionProposalApplied(ClonkCompletionProposal proposal) {}
	
	public void refreshSyntaxColoring() {
		((ClonkSourceViewerConfiguration<?>) getSourceViewerConfiguration()).refreshSyntaxColoring();
	}
	
	/**
	 * Given a {@link ISourceViewer}, look for the corresponding {@link ClonkTextEditor}.
	 * @param <T> Return type specified by the passed cls.
	 * @param sourceViewer The {@link ISourceViewer}
	 * @param cls The class of {@link ClonkTextEditor} to return an instance of.
	 * @return The {@link ClonkTextEditor} corresponding to the source viewer and being of the required class or null.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ClonkTextEditor> T getEditorForSourceViewer(ISourceViewer sourceViewer, Class<T> cls) {
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference reference : page.getEditorReferences()) {
					IEditorPart editor = reference.getEditor(false);
					if (editor != null && cls.isAssignableFrom(editor.getClass())) {
						if (((ClonkTextEditor) editor).getSourceViewer().equals(sourceViewer)) {
							return (T) editor;
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Return an existing editor for the specified {@link IResource}.
	 * @param <T> Return type specified by the passed cls.
	 * @param resource The {@link IResource} to obtain a matching existing editor for.
	 * @param cls The class of {@link ClonkTextEditor} to return an instance of.
	 * @return The {@link ClonkTextEditor} being opened for the passed resource and being of the required class or null.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ClonkTextEditor> T getEditorForResource(IResource resource, Class<T> cls) {
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference reference : page.getEditorReferences()) {
					IEditorPart editor = reference.getEditor(false);
					if (editor != null && cls.isAssignableFrom(editor.getClass())) {
						if (editor.getEditorInput() instanceof FileEditorInput && ((FileEditorInput)editor.getEditorInput()).getFile().equals(resource)) {
							return (T) editor;
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Relax protectedness of {@link #getSourceViewer()}
	 * @return
	 */
	public final ISourceViewer getProtectedSourceViewer() {
		return super.getSourceViewer();
	}

	@Override
	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();
		if (getTextChangeListener() != null && topLevelDeclaration() instanceof Structure)
			getTextChangeListener().updateStructure((Structure) topLevelDeclaration());
	}
	
	/**
	 * Return the {@link TextChangeListenerBase} object being shared for all editors having opened the same file.
	 * @return
	 */
	protected TextChangeListenerBase<?, ?> getTextChangeListener() {
		return null;
	}
	
}
