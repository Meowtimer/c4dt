package net.arctics.clonk.ui.editors.c4script;

import static net.arctics.clonk.util.Utilities.as;
import static net.arctics.clonk.util.Utilities.fileEditedBy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import net.arctics.clonk.Core;
import net.arctics.clonk.Core.IDocumentAction;
import net.arctics.clonk.index.IHasSubDeclarations;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptParser.VisitCodeFlavour;
import net.arctics.clonk.parser.c4script.DeclarationObtainmentContext;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IMarkerListener;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.IASTVisitor;
import net.arctics.clonk.parser.c4script.ast.IFunctionCall;
import net.arctics.clonk.parser.c4script.ast.ITypeInfo;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.ui.editors.CStylePartitionScanner;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.ExternalScriptsDocumentProvider;
import net.arctics.clonk.ui.editors.IHasEditorPart;
import net.arctics.clonk.ui.editors.TextChangeListenerBase;
import net.arctics.clonk.ui.editors.actions.ClonkTextEditorAction;
import net.arctics.clonk.ui.editors.actions.c4script.FindDuplicatesAction;
import net.arctics.clonk.ui.editors.actions.c4script.FindReferencesAction;
import net.arctics.clonk.ui.editors.actions.c4script.RenameDeclarationAction;
import net.arctics.clonk.ui.editors.actions.c4script.TidyUpCodeAction;
import net.arctics.clonk.ui.editors.actions.c4script.ToggleCommentAction;
import net.arctics.clonk.ui.editors.c4script.C4ScriptSourceViewerConfiguration.C4ScriptContentAssistant;
import net.arctics.clonk.ui.search.C4ScriptSearchAction;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
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
import org.eclipse.ui.texteditor.ITextEditor;
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

	/**
	 * Temporary script that is created when no other script can be found for this editor
	 * @author madeen
	 *
	 */
	private static final class ScratchScript extends Script implements IHasEditorPart {
		private transient final C4ScriptEditor editor;
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

		private ScratchScript(C4ScriptEditor editor) {
			super(new Index() {
				private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
			});
			this.editor = editor;
		}

		@Override
		public IStorage scriptStorage() {
			IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
			try {
				return new SimpleScriptStorage(editor.getEditorInput().toString(), document.get());
			} catch (UnsupportedEncodingException e) {
				return null;
			}
		}

		@Override
		public ITextEditor editorPart() {
			return editor;
		}
	}

	/**
	 * C4Script-specific specialization of {@link TextChangeListenerBase} that tries to only trigger a full reparse of the script when necessary (i.e. not when editing inside of a function)
	 * @author madeen
	 *
	 */
	public final static class TextChangeListener extends TextChangeListenerBase<C4ScriptEditor, Script> {
		
		private static final int REPARSE_DELAY = 700;
		private static final Map<IDocument, TextChangeListenerBase<C4ScriptEditor, Script>> listeners = new HashMap<IDocument, TextChangeListenerBase<C4ScriptEditor,Script>>();
		
		private final Timer reparseTimer = new Timer("ReparseTimer"); //$NON-NLS-1$
		private TimerTask reparseTask, functionReparseTask;
		
		public static TextChangeListener addTo(IDocument document, Script script, C4ScriptEditor client)  {
			try {
				return addTo(listeners, TextChangeListener.class, document, script, client);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
		}
		
		@Override
		public void documentChanged(DocumentEvent event) {
			super.documentChanged(event);
			final Function f = structure.funcAt(event.getOffset());
			if (f != null && !f.isOldStyle())
				// editing inside new-style function: adjust locations of declarations without complete reparse
				// only recheck the function and display problems after delay
				scheduleReparsingOfFunction(f);
			else
				// only schedule reparsing when editing outside of existing function
				scheduleReparsing(true);
		}
		
		@Override
		protected void adjustDec(Declaration declaration, int offset, int add) {
			super.adjustDec(declaration, offset, add);
			if (declaration instanceof Function)
				incrementLocationOffsetsExceedingThreshold(((Function)declaration).bodyLocation(), offset, add);
			for (Declaration v : declaration.subDeclarations(declaration.index(), IHasSubDeclarations.ALL))
				adjustDec(v, offset, add);
		}

		public void scheduleReparsing(final boolean onlyDeclarations) {
			reparseTask = cancelTimerTask(reparseTask);
			if (structure == null)
				return;
			reparseTimer.schedule(reparseTask = new TimerTask() {
				@Override
				public void run() {
					if (!ClonkPreferences.toggle(ClonkPreferences.SHOW_ERRORS_WHILE_TYPING, true))
						return;
					try {
						try {
							reparseWithDocumentContents(null, onlyDeclarations, document, structure, new Runnable() {
								@Override
								public void run() {
									for (C4ScriptEditor ed : clients) {
										ed.refreshOutline();
										ed.handleCursorPositionChanged();
									}
								}
							});
						} finally {
							cancel();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, REPARSE_DELAY);
		}
		
		public static void removeMarkers(Function func, Script script) {
			if (script != null && script.resource() != null)
				try {
					// delete all "while typing" errors
					IMarker[] markers = script.resource().findMarkers(Core.MARKER_C4SCRIPT_ERROR_WHILE_TYPING, false, 3);
					for (IMarker m : markers)
						m.delete();
					// delete regular markers that are in the region of interest
					markers = script.resource().findMarkers(Core.MARKER_C4SCRIPT_ERROR, false, 3);
					SourceLocation body = func != null ? func.bodyLocation() : null;
					for (IMarker m : markers) {
						
						// delete markers that are explicitly marked as being caused by parsing the function
						if (func.makeNameUniqueToParent().equals(ParserErrorCode.declarationTag(m))) {
							m.delete();
							continue;
						}
						
						// delete marks inside the body region
						int markerStart = m.getAttribute(IMarker.CHAR_START, 0);
						int markerEnd   = m.getAttribute(IMarker.CHAR_END, 0);
						if (body == null || (markerStart >= body.start() && markerEnd < body.end())) {
							m.delete();
							continue;
						}
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
		}
		
		private void scheduleReparsingOfFunction(final Function fn) {
			functionReparseTask = cancelTimerTask(functionReparseTask);
			reparseTimer.schedule(functionReparseTask = new TimerTask() {
				@Override
				public void run() {
					if (!ClonkPreferences.toggle(ClonkPreferences.SHOW_ERRORS_WHILE_TYPING, true))
						return;
					removeMarkers(fn, structure);
					if (structure.scriptStorage() instanceof IResource && C4GroupItem.groupItemBackingResource((IResource) structure.scriptStorage()) == null) {
						final Function f = (Function) fn.latestVersion();
						C4ScriptParser.visitCode(document, structure, f, null, new IMarkerListener() {
							@Override
							public Decision markerEncountered(C4ScriptParser parser, ParserErrorCode code,
									int markerStart, int markerEnd, int flags,
									int severity, Object... args) {
								if (!parser.errorEnabled(code))
									return Decision.DropCharges;
								if (structure.scriptStorage() instanceof IFile)
									code.createMarker((IFile) structure.scriptStorage(), structure, Core.MARKER_C4SCRIPT_ERROR_WHILE_TYPING,
										markerStart, markerEnd, severity, parser.convertRelativeRegionToAbsolute(flags, parser.expressionReportingErrors()), args);
								return Decision.PassThrough;
							}
						}, VisitCodeFlavour.AlsoStatements, true);
						for (Variable localVar : f.localVars()) {
							SourceLocation l = localVar.location();
							l.setStart(f.bodyLocation().getOffset()+l.getOffset());
							l.setEnd(f.bodyLocation().getOffset()+l.end());
						}
					}
				}
			}, REPARSE_DELAY);
		}

		@Override
		public void cancelReparsingTimer() {
			reparseTask = cancelTimerTask(reparseTask);
			functionReparseTask = cancelTimerTask(functionReparseTask);
			super.cancelReparsingTimer();
		}
		
		@Override
		public void cleanupAfterRemoval() {
			if (reparseTimer != null)
				reparseTimer.cancel();
			try {
				if (structure.scriptStorage() instanceof IFile) {
					IFile file = (IFile)structure.scriptStorage();
					// might have been closed due to removal of the file - don't cause exception by trying to reparse that file now
					if (file.exists())
						reparseWithDocumentContents(null, false, file, structure, null);
				}
			} catch (ParsingException e) {
				e.printStackTrace();
			}
			super.cleanupAfterRemoval();
		}
		
		public static TextChangeListener listenerFor(IDocument document) {
			return (TextChangeListener) listeners.get(document);
		}
	}

	private final ColorManager colorManager;
	private static final String ENABLE_BRACKET_HIGHLIGHT = Core.id("enableBracketHighlighting"); //$NON-NLS-1$
	private static final String BRACKET_HIGHLIGHT_COLOR = Core.id("bracketHighlightColor"); //$NON-NLS-1$
	
	private final DefaultCharacterPairMatcher fBracketMatcher = new DefaultCharacterPairMatcher(new char[] { '{', '}', '(', ')', '[', ']' });
	private TextChangeListener textChangeListener;
	private WeakReference<DeclarationObtainmentContext> cachedDeclarationObtainmentContext;
	
	@Override
	public DeclarationObtainmentContext declarationObtainmentContext() {
		if (cachedDeclarationObtainmentContext != null) {
			DeclarationObtainmentContext ctx = cachedDeclarationObtainmentContext.get();
			if (ctx != null && ctx.script() == script())
				return ctx;
		}
		cachedDeclarationObtainmentContext = new WeakReference<DeclarationObtainmentContext>(
			parserForDocument(getDocumentProvider().getDocument(getEditorInput()), script())
		);
		return cachedDeclarationObtainmentContext.get();
	}
	
	public C4ScriptEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new C4ScriptSourceViewerConfiguration(getPreferenceStore(), colorManager,this));
		//setDocumentProvider(new ClonkDocumentProvider(this));
	}
	
	public void showContentAssistance() {
		// show parameter help
		ITextOperationTarget opTarget = (ITextOperationTarget) getSourceViewer();
		try {
			if (PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart() == C4ScriptEditor.this) {
				C4ScriptContentAssistant a = as(contentAssistant(), C4ScriptContentAssistant.class);
				if (a != null && !a.isProposalPopupActive())
					if (opTarget.canDoOperation(ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION))
						opTarget.doOperation(ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
			}
		} catch (NullPointerException nullP) {
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
		IDocument document = getDocumentProvider().getDocument(input);
		if (document.getDocumentPartitioner() == null) {
			IDocumentPartitioner partitioner =
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
		cachedScript = new WeakReference<Script>(null);
		super.refreshOutline();
	}
	
	@Override
	protected void refreshStructure() {
		try {
			reparseWithDocumentContents(null, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public IIndexEntity entityAtRegion(boolean fallbackToCurrentFunction, IRegion region) {
		try {
			EntityLocator info = new EntityLocator(
				this,
				this.getDocumentProvider().getDocument(this.getEditorInput()),
				region
				);
			if (info.entity() != null)
				return info.entity();
			else if (fallbackToCurrentFunction)
				return functionAt(region.getOffset());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	protected void editorSaved() {
		if (textChangeListener != null)
			textChangeListener.cancelReparsingTimer();
		if (script() instanceof ScratchScript)
			try {
				reparseWithDocumentContents(null, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
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
		Script script = script();
		if (script != null && script.isEditable())
			textChangeListener = TextChangeListener.addTo(getDocumentProvider().getDocument(getEditorInput()), script, this);
		getSourceViewer().getTextWidget().addMouseListener(showContentAssistAtKeyUpListener);
		getSourceViewer().getTextWidget().addKeyListener(showContentAssistAtKeyUpListener);
	}

	@Override
	public void dispose() {
		if (textChangeListener != null) {
			textChangeListener.removeClient(this);
			textChangeListener = null;
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
			C4ScriptSearchAction.class
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
		Function f = functionAtCursor();
		boolean noHighlight = true;
		if (f != null) {
			this.setHighlightRange(f.location().getOffset(), Math.min(
				f.bodyLocation().getOffset()-f.location().getOffset() + f.bodyLocation().getLength() + (f.isOldStyle()?0:1),
				this.getDocumentProvider().getDocument(getEditorInput()).getLength()-f.location().getOffset()
			), false);
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
			reparseWithDocumentContents(null, true);
		} catch (IOException | ParsingException e) {
			e.printStackTrace();
		}
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() { showContentAssistance(); }
		});
		super.completionProposalApplied(proposal);
	}

	/**
	 *  Created if there is no suitable script to get from somewhere else
	 *  can be considered a hack to make viewing (svn) revisions of a file work
	 */
	private WeakReference<Script> cachedScript = new WeakReference<Script>(null);
	
	public Script script() {
		Script result = cachedScript.get();
		if (result != null)
			return result;
		
		if (getEditorInput() instanceof ScriptWithStorageEditorInput)
			result = ((ScriptWithStorageEditorInput)getEditorInput()).script();

		if (result == null) {
			IFile f = Utilities.fileEditedBy(this);
			if (f != null) {
				Script script = Script.get(f, true);
				if (script != null)
					result = script;
			}
		}

		boolean needsReparsing = false;
		if (result == null && cachedScript.get() == null) {
			result = new ScratchScript(this);
			needsReparsing = true;
		}
		cachedScript = new WeakReference<Script>(result);
		if (needsReparsing)
			try {
				reparseWithDocumentContents(null, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		return result;
	}
	
	@Override
	protected TextChangeListenerBase<?, ?> getTextChangeListener() {
		return textChangeListener;
	}

	public Function functionAt(int offset) {
		Script script = script();
		if (script != null) {
			Function f = script.funcAt(offset);
			return f;
		}
		return null;
	}
	
	public Function functionAtCursor() {
		return functionAt(cursorPos());
	}

	public C4ScriptParser reparseWithDocumentContents(IASTVisitor exprListener, boolean onlyDeclarations) throws IOException, ParsingException {
		if (script() == null)
			return null;
		IDocument document = getDocumentProvider().getDocument(getEditorInput());
		if (textChangeListener != null)
			textChangeListener.cancelReparsingTimer();
		return reparseWithDocumentContents(exprListener, onlyDeclarations, document, script(), new Runnable() {
			@Override
			public void run() {
				refreshOutline();
				handleCursorPositionChanged();
			}
		});
	}

	private static C4ScriptParser reparseWithDocumentContents(
			IASTVisitor exprListener,
			boolean onlyDeclarations, Object document,
			final Script script,
			Runnable uiRefreshRunnable)
			throws ParsingException {
		C4ScriptParser parser = parserForDocument(document, script);
		List<ITypeInfo> storedLocalsTypeInformation = null;
		if (onlyDeclarations) {
			// when only parsing declarations store type information for variables declared in the script
			// and apply that information back to the variables after having reparsed so that type information is kept like it was (resulting from a full parse)
			storedLocalsTypeInformation = new LinkedList<ITypeInfo>();
			for (Variable v : script.variables()) {
				ITypeInfo info = v.type() != null ? AccessVar.makeTypeInfo(v, parser) : null;
				if (info != null)
					storedLocalsTypeInformation.add(info);
			}
		}
		parser.setListener(exprListener);
		parser.clean();
		parser.parseDeclarations();
		parser.script().generateFindDeclarationCache();
		//if (!onlyDeclarations)
			parser.parseCodeOfFunctionsAndValidate();
		if (!onlyDeclarations)
			parser.reportProblems();
		if (storedLocalsTypeInformation != null)
			for (ITypeInfo info : storedLocalsTypeInformation)
				info.apply(false, parser);
		// make sure it's executed on the ui thread
		if (uiRefreshRunnable != null)
			Display.getDefault().asyncExec(uiRefreshRunnable);
		return parser;
	}

	private static C4ScriptParser parserForDocument(Object document, final Script script) {
		C4ScriptParser parser = null;
		if (document instanceof IDocument)
			parser = new C4ScriptParser(((IDocument)document).get(), script, script.scriptFile());
		else if (document instanceof IFile)
			parser = Core.instance().performActionsOnFileDocument((IFile) document, new IDocumentAction<C4ScriptParser>() {
				@Override
				public C4ScriptParser run(IDocument document) {
					return new C4ScriptParser(document.get(), script, script.scriptFile());
				}
			});
		if (parser == null)
			throw new InvalidParameterException("document");
		return parser;
	}
	
	@Override
	public Script topLevelDeclaration() {
		return script();
	}
	
	public static class FuncCallInfo {
		public IFunctionCall callFunc;
		public int parmIndex;
		public int parmsStart, parmsEnd;
		public EntityLocator locator;
		public FuncCallInfo(Function func, IFunctionCall callFunc2, ExprElm parm, EntityLocator locator) {
			this.callFunc = callFunc2;
			this.parmIndex = parm != null ? callFunc2.indexOfParm(parm) : 0;
			this.parmsStart = func.bodyLocation().start()+callFunc2.parmsStart();
			this.parmsEnd = func.bodyLocation().start()+callFunc2.parmsEnd();
			this.locator = locator;
		}
	}

	public FuncCallInfo innermostFunctionCallParmAtOffset(int offset) throws BadLocationException, ParsingException {
		Function f = this.functionAt(offset);
		if (f == null)
			return null;
		EntityLocator locator = new EntityLocator(this, getSourceViewer().getDocument(), new Region(offset, 0));
		ExprElm expr;

		// cursor somewhere between parm expressions... locate CallFunc and search
		int bodyStart = f.bodyLocation().start();
		for (
			expr = locator.expressionAtRegion();
			expr != null;
			expr = expr.parent()
		)
			if (expr instanceof IFunctionCall && offset-bodyStart >= ((IFunctionCall)expr).parmsStart())
				 break;
		if (expr != null) {
			IFunctionCall callFunc = (IFunctionCall) expr;
			ExprElm prev = null;
			for (ExprElm parm : callFunc.params()) {
				if (parm.end() > offset) {
					if (prev == null)
						break;
					String docText = getSourceViewer().getDocument().get(bodyStart+prev.end(), parm.start()-prev.end());
					int commaIndex = docText.indexOf(',');
					return new FuncCallInfo(f, callFunc, offset >= bodyStart+prev.end()+commaIndex ? parm : prev, locator);
				}
				prev = parm;
			}
			return new FuncCallInfo(f, callFunc, prev, locator);
		}
		return null;
	}
	
	@Override
	protected void initializeEditor() {
		IFile file = fileEditedBy(this);
		if (file != null)
			ClonkProjectNature.get(file).index();
		super.initializeEditor();
	}

}
