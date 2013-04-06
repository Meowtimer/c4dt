package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.fileEditedBy;

import java.io.IOException;
import java.util.ResourceBundle;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.parser.ASTNode;
import net.arctics.clonk.parser.CStyleScanner;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.FunctionFragmentParser;
import net.arctics.clonk.parser.c4script.ProblemReportingContext;
import net.arctics.clonk.parser.c4script.ProblemReportingStrategy;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.ast.IFunctionCall;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.ui.editors.CStylePartitionScanner;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.ExternalScriptsDocumentProvider;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction;
import net.arctics.clonk.ui.editors.actions.c4script.EvaluateC4Script;
import net.arctics.clonk.ui.editors.actions.c4script.FindDuplicatesAction;
import net.arctics.clonk.ui.editors.actions.c4script.FindReferencesAction;
import net.arctics.clonk.ui.editors.actions.c4script.RenameDeclarationAction;
import net.arctics.clonk.ui.editors.actions.c4script.TidyUpCodeAction;
import net.arctics.clonk.ui.editors.actions.c4script.ToggleCommentAction;
import net.arctics.clonk.ui.editors.c4script.ScriptEditingState.ReparseFunctionMode;
import net.arctics.clonk.ui.editors.c4script.C4ScriptSourceViewerConfiguration.C4ScriptContentAssistant;
import net.arctics.clonk.ui.search.C4ScriptSearchAction;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

/**
 * Text editor for C4Scripts.
 * @author madeen
 *
 */
public class C4ScriptEditor extends ClonkTextEditor {

	private final class ShowContentAssistAtKeyUpListener implements MouseListener, KeyListener {
		@Override
		public void keyPressed(KeyEvent e) {}
		@Override
		public void mouseDoubleClick(MouseEvent e) {}
		@Override
		public void mouseDown(MouseEvent e) {}
		@Override
		public void keyReleased(KeyEvent e) { showContentAssistance(); }
		@Override
		public void mouseUp(MouseEvent e) { showContentAssistance(); }
	}

	private final ColorManager colorManager;
	private static final String ENABLE_BRACKET_HIGHLIGHT = Core.id("enableBracketHighlighting"); //$NON-NLS-1$
	private static final String BRACKET_HIGHLIGHT_COLOR = Core.id("bracketHighlightColor"); //$NON-NLS-1$

	private final DefaultCharacterPairMatcher fBracketMatcher = new DefaultCharacterPairMatcher(new char[] { '{', '}', '(', ')', '[', ']' });
	private ScriptEditingState editingState;

	public C4ScriptEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new C4ScriptSourceViewerConfiguration(getPreferenceStore(), colorManager,this));
	}

	public void showContentAssistance() {
		// show parameter help
		final ITextOperationTarget opTarget = (ITextOperationTarget) getSourceViewer();
		try {
			if (PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart() == C4ScriptEditor.this) {
				final C4ScriptContentAssistant a = as(contentAssistant(), C4ScriptContentAssistant.class);
				if (a != null && !a.isProposalPopupActive())
					if (opTarget.canDoOperation(ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION))
						opTarget.doOperation(ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
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
		super.doSetInput(input);
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
		editingState().invalidate();
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
				this,
				this.getDocumentProvider().getDocument(this.getEditorInput()),
				region
				);
			if (info.entity() != null)
				return info.entity();
			else if (fallbackToCurrentFunction)
				return functionAt(region.getOffset());
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
		if (editingState != null) {
			editingState.cancelReparsingTimer();
			final Function cursorFunc = functionAtCursor();
			if (cursorFunc != null)
				editingState.reparseFunction(cursorFunc, ReparseFunctionMode.FULL).deploy();
		}
		final C4ScriptContentAssistant a = as(contentAssistant(), C4ScriptContentAssistant.class);
		if (a != null)
			a.hide();
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
		final Script script = Script.get(Utilities.fileEditedBy(this), true);
		if (script != null && script.isEditable())
			editingState = ScriptEditingState.addTo(getDocumentProvider().getDocument(getEditorInput()), script, this);
		super.createPartControl(parent);
		getSourceViewer().getTextWidget().addMouseListener(showContentAssistAtKeyUpListener);
		getSourceViewer().getTextWidget().addKeyListener(showContentAssistAtKeyUpListener);
	}

	@Override
	public void dispose() {
		if (editingState != null) {
			editingState.removeEditor(this);
			editingState = null;
		}
		colorManager.dispose();
		super.dispose();
	}

	public static final ResourceBundle MESSAGES_BUNDLE = ResourceBundle.getBundle(Core.id("ui.editors.c4script.actionsBundle")); //$NON-NLS-1$

	@SuppressWarnings("unchecked")
	@Override
	protected void createActions() {
		super.createActions();
		addActions(MESSAGES_BUNDLE,
			TidyUpCodeAction.class,
			FindReferencesAction.class,
			RenameDeclarationAction.class,
			FindDuplicatesAction.class,
			ToggleCommentAction.class,
			C4ScriptSearchAction.class,
			EvaluateC4Script.class
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
				addAction(menu, ClonkTextEditorAction.idString(RenameDeclarationAction.class));
				addAction(menu, ClonkTextEditorAction.idString(ToggleCommentAction.class));
			}
			addAction(menu, ClonkTextEditorAction.idString(FindReferencesAction.class));
			addAction(menu, ClonkTextEditorAction.idString(FindDuplicatesAction.class));
		}
	}

	public int cursorPos() {
		return ((TextSelection)getSelectionProvider().getSelection()).getOffset();
	}

	@Override
	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();

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
		sourceViewerConfiguration().autoEditStrategy().handleCursorPositionChanged(
			cursorPos(), getDocumentProvider().getDocument(getEditorInput()));

	}

	private final C4ScriptSourceViewerConfiguration sourceViewerConfiguration() {
		return (C4ScriptSourceViewerConfiguration)getSourceViewerConfiguration();
	}

	@Override
	public void completionProposalApplied(ClonkCompletionProposal proposal) {
		sourceViewerConfiguration().autoEditStrategy().completionProposalApplied(proposal);
		try {
			if (proposal.requiresDocumentReparse()) {
				reparse(true);
				editingState().scheduleReparsing(false);
			}
		} catch (IOException | ParsingException e) {
			e.printStackTrace();
		}
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {
				final Function f = functionAtCursor();
				if (f != null)
					editingState().reparseFunction(f, ReparseFunctionMode.FULL).deploy();
				showContentAssistance();
			}
		});
		super.completionProposalApplied(proposal);
	}

	@Override
	protected ScriptEditingState editingState() { return editingState; }

	public ProblemReportingStrategy typingStrategy() { return editingState().typingStrategy(); }

	public Function functionAt(int offset) {
		final Script script = script();
		if (script != null) {
			final Function f = script.funcAt(offset);
			return f;
		}
		return null;
	}

	public Function functionAtCursor() { return functionAt(cursorPos()); }

	public Script script() {
		final ScriptEditingState listener = editingState();
		return listener != null ? listener.structure() : null;
	}
	
	public C4ScriptParser reparse(boolean onlyDeclarations) throws IOException, ParsingException {
		if (script() == null)
			return null;
		final IDocument document = getDocumentProvider().getDocument(getEditorInput());
		if (editingState != null)
			editingState.cancelReparsingTimer();
		return editingState.reparseWithDocumentContents(onlyDeclarations, document, script(), new Runnable() {
			@Override
			public void run() {
				refreshOutline();
				handleCursorPositionChanged();
			}
		});
	}

	public static class FuncCallInfo {
		public IFunctionCall callFunc;
		public int parmIndex;
		public int parmsStart, parmsEnd;
		public EntityLocator locator;
		public FuncCallInfo(Function func, IFunctionCall callFunc2, ASTNode parm, EntityLocator locator) {
			this.callFunc = callFunc2;
			this.parmIndex = parm != null ? callFunc2.indexOfParm(parm) : 0;
			this.parmsStart = func.bodyLocation().start()+callFunc2.parmsStart();
			this.parmsEnd = func.bodyLocation().start()+callFunc2.parmsEnd();
			this.locator = locator;
		}
	}

	public FuncCallInfo innermostFunctionCallParmAtOffset(int offset) throws BadLocationException, ParsingException {
		final Function f = this.functionAt(offset);
		if (f == null)
			return null;
		final FunctionFragmentParser parser = new FunctionFragmentParser(getSourceViewer().getDocument(), script(), f, null);
		parser.update();
		final EntityLocator locator = new EntityLocator(this, getSourceViewer().getDocument(), new Region(offset, 0));
		ASTNode expr;

		// cursor somewhere between parm expressions... locate CallFunc and search
		final int bodyStart = f.bodyLocation().start();
		for (
			expr = locator.expressionAtRegion();
			expr != null;
			expr = expr.parent()
		)
			if (expr instanceof IFunctionCall && offset-bodyStart >= ((IFunctionCall)expr).parmsStart())
				 break;
		if (expr != null) {
			final IFunctionCall callFunc = (IFunctionCall) expr;
			ASTNode prev = null;
			for (final ASTNode parm : callFunc.params()) {
				if (bodyStart+parm.end() > offset) {
					if (prev == null)
						break;
					final String docText = getSourceViewer().getDocument().get(bodyStart+prev.end(), parm.start()-prev.end());
					final CStyleScanner scanner = new CStyleScanner(docText);
					scanner.eatWhitespace();
					final boolean comma = scanner.read() == ',' && offset+1 > bodyStart+prev.end() + scanner.tell();
					return new FuncCallInfo(f, callFunc, comma ? parm : prev, locator);
				}
				prev = parm;
			}
			return new FuncCallInfo(f, callFunc, prev, locator);
		}
		return null;
	}

	@Override
	protected void initializeEditor() {
		final IFile file = fileEditedBy(this);
		if (file != null)
			ClonkProjectNature.get(file).index();
		super.initializeEditor();
	}
	
	@Override
	public ProblemReportingContext declarationObtainmentContext() {
		return editingState().declarationObtainmentContext();
	}

}
