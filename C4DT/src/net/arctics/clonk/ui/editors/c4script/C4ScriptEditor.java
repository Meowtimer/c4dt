package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.fileEditedBy;

import java.io.IOException;
import java.util.ResourceBundle;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.builder.ClonkProjectNature;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ast.EntityLocator;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.ui.editors.CStylePartitionScanner;
import net.arctics.clonk.ui.editors.ExternalScriptsDocumentProvider;
import net.arctics.clonk.ui.editors.StructureEditingState;
import net.arctics.clonk.ui.editors.StructureTextEditor;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction.CommandId;
import net.arctics.clonk.ui.editors.actions.c4script.EvaluateC4Script;
import net.arctics.clonk.ui.editors.actions.c4script.FindDuplicatesAction;
import net.arctics.clonk.ui.editors.actions.c4script.FindReferencesAction;
import net.arctics.clonk.ui.editors.actions.c4script.TidyUpCodeAction;
import net.arctics.clonk.ui.editors.actions.c4script.ToggleCommentAction;
import net.arctics.clonk.ui.search.ScriptSearchAction;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

/**
 * Text editor for C4Scripts.
 * @author madeen
 *
 */
public class C4ScriptEditor extends StructureTextEditor {

	private boolean showParametersEnabled = true;

	private final class ShowContentAssistAtKeyUpListener implements MouseListener, KeyListener {
		@Override
		public void keyPressed(KeyEvent e) {}
		@Override
		public void keyReleased(KeyEvent e) {
			showParameters();
		}
		@Override
		public void mouseDoubleClick(MouseEvent e) {}
		@Override
		public void mouseDown(MouseEvent e) {}
		@Override
		public void mouseUp(MouseEvent e) { showParameters(); }
	}

	@CommandId(id="ui.editors.actions.ToggleParametersShown")
	public static class ToggleParametersShown extends ClonkTextEditorAction {
		private C4ScriptEditor ed() { return (C4ScriptEditor)getTextEditor(); }
		public ToggleParametersShown(ResourceBundle bundle, String prefix, ITextEditor editor) {
			super(bundle, prefix, editor);
		}
		@Override
		public void run() {
			final C4ScriptEditor ed = ed();
			if (ed.showParametersEnabled = !ed.showParametersEnabled)
				ed.showParameters();
			else
				ed.hideContentAssistance();
		}
	}

	private static final String ENABLE_BRACKET_HIGHLIGHT = Core.id("enableBracketHighlighting"); //$NON-NLS-1$
	private static final String BRACKET_HIGHLIGHT_COLOR = Core.id("bracketHighlightColor"); //$NON-NLS-1$

	private final DefaultCharacterPairMatcher fBracketMatcher = new DefaultCharacterPairMatcher(new char[] { '{', '}', '(', ')', '[', ']' });
	private ScriptEditingState state;

	public C4ScriptEditor() { super(); }

	public void hideContentAssistance() {
		state().assistant().hide();
	}

	public boolean showParametersEnabled() { return showParametersEnabled; }

	public void showParameters() {
		// show parameter help
		if (!showParametersEnabled)
			return;
		try {
			if (PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart() == C4ScriptEditor.this) {
				final ScriptEditingState.Assistant a = as(contentAssistant(), ScriptEditingState.Assistant.class);
				if (a != null && !a.isProposalPopupActive())
					a.showParameters((ITextOperationTarget)getSourceViewer());
			}
		} catch (final NullPointerException nullP) {
			// might just be not that much of an issue
		}
	}

	@Override
	protected void setDocumentProvider(IEditorInput input) {
		if (input instanceof ScriptWithStorageEditorInput)
			setDocumentProvider(new ExternalScriptsDocumentProvider(this));
		else
			super.setDocumentProvider(input);
	}

	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		try {
			super.doSetInput(input);
		} finally {
			setDocumentPartitioner(input);
		}
	}

	private void setDocumentPartitioner(IEditorInput input) {
		final IDocument document = getDocumentProvider().getDocument(input);
		if (document.getDocumentPartitioner() == null) {
			final IDocumentPartitioner partitioner =
				new FastPartitioner(
					new CStylePartitionScanner(),
					CStylePartitionScanner.PARTITIONS
				);
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
	}

	@Override
	public void refreshOutline() {
		final ScriptEditingState state = state();
		if (state != null)
			state.invalidate();
		super.refreshOutline();
	}

	@Override
	protected void refreshStructure() {
		try {
			reparse(false);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public IIndexEntity entityAtRegion(boolean fallbackToCurrentFunction, IRegion region) {
		try {
			final EntityLocator info = new EntityLocator(
				script(),
				this.getDocumentProvider().getDocument(this.getEditorInput()),
				region
			);
			if (info.entity() != null)
				return info.entity();
			else if (fallbackToCurrentFunction)
				return state().functionAt(region.getOffset());
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void editorSaved() {
		if (script() instanceof ScratchScript)
			try {
				reparse(false);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		final ScriptEditingState state = state();
		if (state != null)
			state.saved();
		super.editorSaved();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#configureSourceViewerDecorationSupport(org.eclipse.ui.texteditor.SourceViewerDecorationSupport)
	 */
	@Override
	protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
		super.configureSourceViewerDecorationSupport(support);
		support.setCharacterPairMatcher(fBracketMatcher);
		support.setMatchingCharacterPainterPreferenceKeys(ENABLE_BRACKET_HIGHLIGHT, BRACKET_HIGHLIGHT_COLOR);
		getPreferenceStore().setValue(ENABLE_BRACKET_HIGHLIGHT, true);
		PreferenceConverter.setValue(getPreferenceStore(), BRACKET_HIGHLIGHT_COLOR, new RGB(0x33,0x33,0xAA));
	}

	private final ShowContentAssistAtKeyUpListener showContentAssistAtKeyUpListener = new ShowContentAssistAtKeyUpListener();

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		getSourceViewer().getTextWidget().addMouseListener(showContentAssistAtKeyUpListener);
		getSourceViewer().getTextWidget().addKeyListener(showContentAssistAtKeyUpListener);
	}

	public static final ResourceBundle MESSAGES_BUNDLE = ResourceBundle.getBundle(Core.id("ui.editors.c4script.actionsBundle")); //$NON-NLS-1$

	@SuppressWarnings("unchecked")
	@Override
	protected void createActions() {
		super.createActions();
		addActions(MESSAGES_BUNDLE,
			TidyUpCodeAction.class,
			FindReferencesAction.class,
			FindDuplicatesAction.class,
			ToggleCommentAction.class,
			ScriptSearchAction.class,
			EvaluateC4Script.class,
			ToggleParametersShown.class
		);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#editorContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		if (script() != null) {
			if (script().isEditable()) {
				addAction(menu, ClonkTextEditorAction.idString(TidyUpCodeAction.class));
				addAction(menu, ClonkTextEditorAction.idString(ToggleCommentAction.class));
			}
			addAction(menu, ClonkTextEditorAction.idString(FindReferencesAction.class));
			addAction(menu, ClonkTextEditorAction.idString(FindDuplicatesAction.class));
		}
	}

	@Override
	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();
		final ScriptEditingState state = state();
		if (state == null)
			return;
		// highlight active function
		final Function f = functionAtCursor();
		boolean noHighlight = true;
		if (f != null) {
			this.setHighlightRange(f.start(), f.getLength(), false);
			noHighlight = false;
		}
		if (noHighlight)
			this.resetHighlightRange();
		// inform auto edit strategy about cursor position change so it can delete its override regions
		state.autoEditStrategy().removeOverrideRegionsNotAtLine(
			cursorPos(), getDocumentProvider().getDocument(getEditorInput()));
	}

	@Override
	public ScriptEditingState state() {
		try {
			final Script script = Script.get(Utilities.fileEditedBy(this), true);
			if (state == null && script != null && script.isEditable())
				state = StructureEditingState.request(
					ScriptEditingState.class, getDocumentProvider().getDocument(getEditorInput()),
					script, this
				);
			setSourceViewerConfiguration(state);
			return state;
		} catch (final Exception e) {
			return state = null;
		}
	}

	public Function functionAtCursor() {
		try {
			return state().functionAt(cursorPos());
		} catch (final NullPointerException np) {
			return null;
		}
	}

	@Override
	public ASTNode section() { return functionAtCursor(); }

	public Script script() {
		final ScriptEditingState state = state();
		return state != null ? state.structure() : null;
	}

	public void reparse(boolean onlyDeclarations) throws IOException, ProblemException {
		if (script() == null)
			return;
		if (state != null)
			state.cancelReparsingTimer();
		state.reparseWithDocumentContents(new Runnable() {
			@Override
			public void run() {
				refreshOutline();
				handleCursorPositionChanged();
			}
		});
	}

	@Override
	protected void initializeEditor() {
		final IFile file = fileEditedBy(this);
		if (file != null)
			ClonkProjectNature.get(file).index();
		super.initializeEditor();
	}

}
