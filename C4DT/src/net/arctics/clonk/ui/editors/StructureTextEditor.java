package net.arctics.clonk.ui.editors;

import java.util.ResourceBundle;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.DeclarationLocation;
import net.arctics.clonk.ast.IASTSection;
import net.arctics.clonk.ast.IASTVisitor;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.ast.TraversalContinuation;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction.CommandId;
import net.arctics.clonk.ui.editors.actions.OpenDeclarationAction;
import net.arctics.clonk.ui.editors.actions.c4script.RenameDeclarationAction;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension2;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
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
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * Base class for Clonk text editors.
 * @author madeen
 *
 */
public class StructureTextEditor extends TextEditor {

	protected StructureOutlinePage outlinePage;
	private ShowInAdapter showInAdapter;

	/**
	 * Select and reveal some location in the text file.
	 * @param location
	 */
	public void selectAndReveal(IRegion location) {
		this.selectAndReveal(location.getOffset(), location.getLength());
	}

	public void selectAndRevealLine(int line) {
		final IDocument d = getSourceViewer().getDocument();
		try {
			final IRegion r = new Region(d.getLineOffset(line), 0);
			this.selectAndReveal(r);
		} catch (final BadLocationException e) {
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
	 * Refresh the outline so the new contents of the {@link #structure()} will be shown.
	 */
	public void refreshOutline() {
		if (structure() == null)
			return;
		if (outlinePage != null) // don't start lazy loading of outlinePage
			outlinePage.refresh();
	}

	/**
	 * Return the outline page of this text editor.
	 * @return
	 */
	public StructureOutlinePage outlinePage() {
		if (outlinePage == null) {
			outlinePage = new StructureOutlinePage();
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
		if (adapter.equals(IContentOutlinePage.class))
			return outlinePage();
		if (adapter.equals(IShowInSource.class) || adapter.equals(IShowInTargetList.class)) {
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
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final IWorkbenchPage workbenchPage = workbench.getActiveWorkbenchWindow().getActivePage();
		final Structure structure = target.topLevelStructure();
		if (structure instanceof IHasEditorPart) {
			final IEditorPart ed = ((IHasEditorPart) structure).editorPart();
			revealInEditor(target, structure, ed);
			return ed;
		}
		if (structure != null) {
			final IEditorInput input = structure.makeEditorInput();
			if (input != null)
				try {
					final IEditorDescriptor descriptor = input instanceof IFileEditorInput ? IDE.getEditorDescriptor(((IFileEditorInput)input).getFile()) : null;
					if (descriptor != null) {
						final IEditorPart editor = IDE.openEditor(workbenchPage, input, descriptor.getId(), activate);
						revealInEditor(target, structure, editor);
						return editor;
					} else
						return null;
				} catch (final PartInitException e) {
					e.printStackTrace();
				}
			else if (structure instanceof Definition) {
				final Definition obj = (Definition) structure;
				final IFile defCore = obj.defCoreFile();
				if (defCore != null)
					try {
						IDE.openEditor(workbenchPage, defCore);
					} catch (final PartInitException e) {
						e.printStackTrace();
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
				final IEditorDescriptor descriptor = IDE.getEditorDescriptor((IFile) location.resource());
				ed = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), (IFile) location.resource(), descriptor.getId());
			}
			else if (location.resource() instanceof IContainer) {
				final Definition def = Definition.definitionCorrespondingToFolder((IContainer) location.resource());
				if (def != null)
					ed = openDeclaration(def);
			}
			if (ed instanceof StructureTextEditor)
				((StructureTextEditor) ed).selectAndReveal(location.location());
			return ed;
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected void refreshStructure() {}

	public IIndexEntity entityAtRegion(boolean fallbackToCurrentFunction, IRegion r) {
		return null;
	}

	/**
	 * Reveal a {@link Declaration} in an editor.
	 * @param target The {@link Declaration} to reveal
	 * @param structure The {@link Structure} the {@link Declaration} is contained in.
	 * @param editor The editor to reveal the {@link Declaration} in. Special treatment for {@link StructureTextEditor}, fallback for {@link AbstractTextEditor}.
	 */
	private static void revealInEditor(Declaration target, Structure structure, IEditorPart editor) {
		if (editor instanceof StructureTextEditor) {
			final StructureTextEditor clonkTextEditor = (StructureTextEditor) editor;
			if (target != structure) {
				final Declaration old = target;
				target = target.latestVersion();
				if (target == null)
					target = old;
				if (target != null)
					clonkTextEditor.selectAndReveal(target.regionToSelect());
			}
		} else if (editor instanceof AbstractTextEditor) {
			final AbstractTextEditor ed = (AbstractTextEditor) editor;
			ed.selectAndReveal(target.start(), target.getLength());
		}
	}

	/**
	 * Return the declaration that represents the file being edited
	 * @return the declaration
	 */
	public Declaration structure() {
		final StructureEditingState<?, ?> state = state();
		return state != null ? state.structure() : null;
	}

	public int cursorPos() {
		return ((TextSelection)getSelectionProvider().getSelection()).getOffset();
	}

	public ASTNode section() {
		final Declaration structure = structure();
		if (structure != null) {
			class SectionFinder implements IASTVisitor<Void> {
				int cursorPos = cursorPos();
				ASTNode section = null;
				@Override
				public TraversalContinuation visitNode(ASTNode node, Void context) {
					if (node instanceof IASTSection) {
						final IRegion abs = node.absolute();
						if (cursorPos > abs.getOffset() && cursorPos < abs.getOffset()+abs.getLength()) {
							section = node;
							return TraversalContinuation.TraverseSubElements;
						}
					}
					return section != null ? TraversalContinuation.TraverseSubElements : TraversalContinuation.Continue;
				}
			};
			final SectionFinder finder = new SectionFinder();
			structure.traverse(finder, null);
			return finder.section;
		} else
			return null;
	}

	public static final ResourceBundle MESSAGES_BUNDLE = ResourceBundle.getBundle(Core.id("ui.editors.actionsBundle")); //$NON-NLS-1$

	@SuppressWarnings("unchecked")
	protected void addActions(ResourceBundle messagesBundle, Class<? extends ClonkTextEditorAction>... classes) {
		for (final Class<? extends ClonkTextEditorAction> c : classes) {
			final CommandId id = ClonkTextEditorAction.id(c);
			if (c != null) {
				final String actionName = id.id().substring(id.id().lastIndexOf('.')+1);
				try {
					final ClonkTextEditorAction action = c.getConstructor(ResourceBundle.class, String.class, ITextEditor.class).
						newInstance(messagesBundle, actionName+".", this);
					setAction(action.getActionDefinitionId(), action);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void createActions() {
		super.createActions();
		addActions(MESSAGES_BUNDLE, OpenDeclarationAction.class, RenameDeclarationAction.class);
		if (getSourceViewerConfiguration().getContentAssistant(getSourceViewer()) != null) {
			final IAction action = new ContentAssistAction(MESSAGES_BUNDLE, "ContentAssist.", this); //$NON-NLS-1$
			action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
			setAction(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, action);
		}
	}

	@Override
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		if (structure() != null) {
			menu.add(new Separator(Core.MENU_GROUP_CLONK));
			addAction(menu, ClonkTextEditorAction.idString(OpenDeclarationAction.class));
			addAction(menu, ClonkTextEditorAction.idString(RenameDeclarationAction.class));
		}
	}

	/**
	 * Invoke {@link #hyperlinkAtOffset(int)} using the current selection offset.
	 * @return The hyperlink returned by {@link #hyperlinkAtOffset(int)}
	 */
	public IHyperlink hyperlinkAtCurrentSelection() {
	    final ITextSelection selection = (ITextSelection) this.getSelectionProvider().getSelection();
		final IHyperlink hyperlink = state().hyperlinkAtOffset(selection.getOffset());
		return hyperlink;
	}

	public IPreferenceStore preferenceStore() { return getPreferenceStore(); }

	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		updatePartName();
		if (state() != null)
			state().invalidate();
	}

	private void updatePartName() {
		// set part name to reflect the folder the file is in
		final IResource res = (IResource) getEditorInput().getAdapter(IResource.class);
		if (res != null && res.getParent() != null)
			setPartName(res.getParent().getName() + "/" + res.getName()); //$NON-NLS-1$
	}

	@Override
	protected void initializeEditor() {
		super.initializeEditor();
		setPreferenceStore(EditorsUI.getPreferenceStore());
	}

	public ContentAssistant contentAssistant() { return state().getContentAssistant(getSourceViewer()); }

	public void completionProposalApplied(DeclarationProposal proposal) {}

	/**
	 * Given a {@link ISourceViewer}, look for the corresponding {@link StructureTextEditor}.
	 * @param <T> Return type specified by the passed cls.
	 * @param sourceViewer The {@link ISourceViewer}
	 * @param cls The class of {@link StructureTextEditor} to return an instance of.
	 * @return The {@link StructureTextEditor} corresponding to the source viewer and being of the required class or null.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends StructureTextEditor> T getEditorForSourceViewer(ISourceViewer sourceViewer, Class<T> cls) {
		for (final IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows())
			for (final IWorkbenchPage page : window.getPages())
				for (final IEditorReference reference : page.getEditorReferences()) {
					final IEditorPart editor = reference.getEditor(false);
					if (editor != null && cls.isAssignableFrom(editor.getClass()))
						if (((StructureTextEditor) editor).getSourceViewer().equals(sourceViewer))
							return (T) editor;
				}
		return null;
	}

	/**
	 * Return an existing editor for the specified {@link IResource}.
	 * @param <T> Return type specified by the passed cls.
	 * @param resource The {@link IResource} to obtain a matching existing editor for.
	 * @param cls The class of {@link StructureTextEditor} to return an instance of.
	 * @return The {@link StructureTextEditor} being opened for the passed resource and being of the required class or null.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends StructureTextEditor> T getEditorForResource(IResource resource, Class<T> cls) {
		for (final IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows())
			for (final IWorkbenchPage page : window.getPages())
				for (final IEditorReference reference : page.getEditorReferences()) {
					final IEditorPart editor = reference.getEditor(false);
					if (editor != null && cls.isAssignableFrom(editor.getClass()))
						if (editor.getEditorInput() instanceof FileEditorInput && ((FileEditorInput)editor.getEditorInput()).getFile().equals(resource))
							return (T) editor;
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

	/**
	 * Return the {@link StructureEditingState} object being shared for all editors on the same file.
	 * @return The {@link StructureEditingState}
	 */
	public StructureEditingState<?, ?> state() { return null; }

	@Override
	protected void initializeKeyBindingScopes() {
		super.initializeKeyBindingScopes();
		setKeyBindingScopes(new String[] { Core.CONTEXT_ID });
	}

	public void reconfigureSourceViewer() {
		final ISourceViewer viewer= getSourceViewer();

		if (!(viewer instanceof ISourceViewerExtension2))
			return; // cannot unconfigure - do nothing

		((ISourceViewerExtension2)viewer).unconfigure();
		viewer.configure(getSourceViewerConfiguration());
	}

	// projection support
	protected ProjectionSupport projectionSupport;
	protected ProjectionAnnotationModel projectionAnnotationModel;
	protected Annotation[] oldAnnotations;

	protected void initializeProjectionSupport() {
		final ProjectionViewer projectionViewer = (ProjectionViewer) getSourceViewer();
		projectionSupport = new ProjectionSupport(projectionViewer, getAnnotationAccess(), getSharedColors());
		projectionSupport.install();
		projectionViewer.doOperation(ProjectionViewer.TOGGLE);
		projectionAnnotationModel = projectionViewer.getProjectionAnnotationModel();
	}

	@Override
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		final ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
		getSourceViewerDecorationSupport(viewer);
		return viewer;
	}

	@Override
	public void createPartControl(Composite parent) {
		state();
		super.createPartControl(parent);
		initializeProjectionSupport();
	}

}
