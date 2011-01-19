package net.arctics.clonk.ui.editors.c4script;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.IHasSubDeclarations;
import net.arctics.clonk.parser.c4script.C4ScriptParser.ExpressionsAndStatementsReportingFlavour;
import net.arctics.clonk.parser.c4script.C4ScriptParser.IMarkerListener;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.ast.AccessVar;
import net.arctics.clonk.parser.c4script.ast.Block;
import net.arctics.clonk.parser.c4script.ast.CallFunc;
import net.arctics.clonk.parser.c4script.ast.ExprElm;
import net.arctics.clonk.parser.c4script.ast.IScriptParserListener;
import net.arctics.clonk.parser.c4script.ast.IStoredTypeInformation;
import net.arctics.clonk.parser.c4script.ast.Statement;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal;
import net.arctics.clonk.ui.editors.ClonkPartitionScanner;
import net.arctics.clonk.ui.editors.ExternalScriptsDocumentProvider;
import net.arctics.clonk.ui.editors.IClonkCommandIds;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.IHasEditorRefWhichEnablesStreamlinedOpeningOfDeclarations;
import net.arctics.clonk.ui.editors.TextChangeListenerBase;
import net.arctics.clonk.ui.editors.actions.c4script.FindDuplicateAction;
import net.arctics.clonk.ui.editors.actions.c4script.TidyUpCodeAction;
import net.arctics.clonk.ui.editors.actions.c4script.FindReferencesAction;
import net.arctics.clonk.ui.editors.actions.c4script.RenameDeclarationAction;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
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

public class C4ScriptEditor extends ClonkTextEditor {

	/**
	 * Temporary script that is created when no other script can be found for this editor
	 * @author madeen
	 *
	 */
	private static final class ScratchScript extends ScriptBase implements IHasEditorRefWhichEnablesStreamlinedOpeningOfDeclarations {
		private transient final C4ScriptEditor me;
		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
		
		private static ClonkIndex scratchIndex = new ClonkIndex() {
			private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
		};

		private ScratchScript(C4ScriptEditor me) {
			this.me = me;
		}

		@Override
		public ClonkIndex getIndex() {
			return scratchIndex;
		}

		@Override
		public IStorage getScriptStorage() {
			IDocument document = me.getDocumentProvider().getDocument(me.getEditorInput());
			try {
				return new SimpleScriptStorage(me.getEditorInput().toString(), document.get());
			} catch (UnsupportedEncodingException e) {
				return null;
			}
		}

		@Override
		public ITextEditor getEditor() {
			return me;
		}
	}

	/**
	 * Helper class that takes care of triggering a timed reparsing when the document is changed.
	 * It tries to only fire a full reparse when necessary (i.e. not when editing inside of a function)
	 * @author madeen
	 *
	 */
	public final static class TextChangeListener extends TextChangeListenerBase<C4ScriptEditor, ScriptBase> {
		
		/**
		 * Parser responsible for parsing the edited statement that will then be inserted into
		 * the existing cached function code block. Avoiding reparsing the whole function code
		 * every time the function code is changed should help performance with large scripts.
		 * @author madeen
		 *
		 */
		private static class PatchParser extends C4ScriptParser {
			public PatchParser(ScriptBase script) {
				super(script);
			}
			@Override
			public int bodyOffset() {
				return -offsetOfScriptFragment; // add original statement start so warnings like VarUsedBeforeItsDeclaration won't fire
			};
			@Override
			public boolean errorEnabled(ParserErrorCode error) {
				return false;
			}
			public void setStatementStart(int start) {
				offsetOfScriptFragment = start;
			}
		}
		
		private static final int REPARSE_DELAY = 700;

		private static final Map<IDocument, TextChangeListenerBase<C4ScriptEditor, ScriptBase>> listeners = new HashMap<IDocument, TextChangeListenerBase<C4ScriptEditor,ScriptBase>>();
		
		private Timer reparseTimer = new Timer("ReparseTimer"); //$NON-NLS-1$
		private TimerTask reparseTask, functionReparseTask;
		private PatchParser patchParser;
		
		public static TextChangeListener addTo(IDocument document, ScriptBase script, C4ScriptEditor client)  {
			try {
				return addTo(listeners, TextChangeListener.class, document, script, client);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
		@Override
		protected void added() {
			super.added();
			try {
				patchParser = null;// new PatchParser(structure);
			} catch (Exception e) {
				// ignore
			}
		}

		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {
			try {
				patchFuncBlockAccordingToDocumentChange(event);
			} catch (Exception e) {
				// pfft, have fun reparsing, comput0r
			}
		}
		
		private void patchFuncBlockAccordingToDocumentChange(DocumentEvent event) throws BadLocationException, ParsingException {
			if (patchParser == null)
				return;
			Function f = structure.funcAt(event.getOffset());
			if (f != null) {
				Block originalBlock = f.getCodeBlock();
				if (originalBlock != null) {
					ExpressionLocator locator = new ExpressionLocator(new Region(event.getOffset()-f.getBody().getStart(), event.getLength()));
					originalBlock.traverse(locator);
					ExprElm foundExpression = locator.getExprAtRegion();
					if (foundExpression != null) {
						final Statement originalStatement = foundExpression.getParent(Statement.class);
						int absoluteOffsetToStatement = f.getBody().getOffset()+originalStatement.getExprStart();
						String originalStatementText = event.getDocument().get(absoluteOffsetToStatement, originalStatement.getLength());
						StringBuilder patchStatementTextBuilder = new StringBuilder(originalStatementText);
						patchStatementTextBuilder.delete(event.getOffset()-absoluteOffsetToStatement, event.getOffset()-absoluteOffsetToStatement+event.getLength());
						patchStatementTextBuilder.insert(event.getOffset()-absoluteOffsetToStatement, event.getText());
						String patchStatementText = patchStatementTextBuilder.toString();
						patchParser.setStatementStart(originalStatement.getExprStart());
						Statement patchStatement = patchParser.parseStandaloneStatement(patchStatementText, f, null);
						if (patchStatement != null) {
							originalStatement.getParent().replaceSubElement(originalStatement, patchStatement, patchStatementText.length() - originalStatementText.length());
							StringBuilder wholeFuncBodyBuilder = new StringBuilder(event.getDocument().get(f.getBody().getStart(), f.getBody().getLength()));
							wholeFuncBodyBuilder.replace(originalStatement.getExprStart(), originalStatement.getExprEnd(), patchStatementText);
							f.storeBlock(originalBlock, wholeFuncBodyBuilder.toString());
						}
					}
				}
			}
		}
		
		public void documentChanged(DocumentEvent event) {
			super.documentChanged(event);
			final Function f = structure.funcAt(event.getOffset());
			if (f != null && !f.isOldStyle()) {
				// editing inside new-style function: adjust locations of declarations without complete reparse
				// only recheck the function and display problems after delay
				scheduleReparsingOfFunction(f);
			} else {
				// only schedule reparsing when editing outside of existing function
				scheduleReparsing(true);
			}
		}
		
		@Override
		protected void adjustDec(Declaration declaration, int offset, int add) {
			super.adjustDec(declaration, offset, add);
			if (declaration instanceof Function) {
				addToLocation(((Function)declaration).getBody(), offset, add);
			}
			for (Declaration v : declaration.allSubDeclarations(IHasSubDeclarations.ALL_SUBDECLARATIONS)) {
				adjustDec(v, offset, add);
			}
		}

		public void scheduleReparsing(final boolean onlyDeclarations) {
			reparseTask = cancel(reparseTask);
			if (structure == null)
				return;
			reparseTimer.schedule(reparseTask = new TimerTask() {
				@Override
				public void run() {
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
		
		public static void removeMarkers(Function func, ScriptBase script) {
			if (script != null && script.getResource() != null) {
				try {
					// delete all "while typing" errors
					IMarker[] markers = script.getResource().findMarkers(ClonkCore.MARKER_C4SCRIPT_ERROR_WHILE_TYPING, false, 3);
					for (IMarker m : markers) {
						m.delete();
					}
					// delete regular markers that are in the region of interest
					markers = script.getResource().findMarkers(ClonkCore.MARKER_C4SCRIPT_ERROR, false, 3);
					SourceLocation body = func != null ? func.getBody() : null;
					for (IMarker m : markers) {
						int markerStart = m.getAttribute(IMarker.CHAR_START, 0);
						int markerEnd   = m.getAttribute(IMarker.CHAR_END, 0);
						if (body == null || (markerStart >= body.getStart() && markerEnd < body.getEnd())) {
							m.delete();
						}
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
		
		private void scheduleReparsingOfFunction(final Function fn) {
			functionReparseTask = cancel(functionReparseTask);
			reparseTimer.schedule(functionReparseTask = new TimerTask() {
				@Override
				public void run() {
					removeMarkers(fn, structure);
					if (structure.getScriptStorage() instanceof IResource && !C4GroupItem.isLinkedResource((IResource) structure.getScriptStorage())) {
						final Function f = (Function) fn.latestVersion();
						C4ScriptParser.reportExpressionsAndStatements(document, structure, f, null, new IMarkerListener() {
							@Override
							public WhatToDo markerEncountered(C4ScriptParser parser, ParserErrorCode code,
									int markerStart, int markerEnd, boolean noThrow,
									int severity, Object... args) {
								if (!parser.errorEnabled(code))
									return WhatToDo.DropCharges;
								if (structure.getScriptStorage() instanceof IFile) {
									code.createMarker((IFile) structure.getScriptStorage(), structure, ClonkCore.MARKER_C4SCRIPT_ERROR_WHILE_TYPING,
										markerStart, markerEnd, severity, parser.convertRelativeRegionToAbsolute(parser.getExpressionReportingErrors()), args);
								}
								return WhatToDo.PassThrough;
							}
						}, ExpressionsAndStatementsReportingFlavour.AlsoStatements, true);
						for (Variable localVar : f.getLocalVars()) {
							SourceLocation l = localVar.getLocation();
							l.setStart(f.getBody().getOffset()+l.getOffset());
							l.setEnd(f.getBody().getOffset()+l.getEnd());
						}
					}
				}
			}, REPARSE_DELAY);
		}

		@Override
		public void cancel() {
			reparseTask = cancel(reparseTask);
			functionReparseTask = cancel(functionReparseTask);
			super.cancel();
		}
		
		@Override
		public void cleanupAfterRemoval() {
			try {
				if (structure.getScriptStorage() instanceof IFile) {
					IFile file = (IFile)structure.getScriptStorage();
					reparseWithDocumentContents(null, false, file, structure, null);
				}
			} catch (ParsingException e) {
				e.printStackTrace();
			}
			super.cleanupAfterRemoval();
		}
		
		public static TextChangeListener getListenerFor(IDocument document) {
			return (TextChangeListener) listeners.get(document);
		}
	}

	private ColorManager colorManager;
	private static final String ENABLE_BRACKET_HIGHLIGHT = ClonkCore.id("enableBracketHighlighting"); //$NON-NLS-1$
	private static final String BRACKET_HIGHLIGHT_COLOR = ClonkCore.id("bracketHighlightColor"); //$NON-NLS-1$
	
	private DefaultCharacterPairMatcher fBracketMatcher = new DefaultCharacterPairMatcher(new char[] { '{', '}', '(', ')', '[', ']' });
	private TextChangeListener textChangeListener;
	
	public C4ScriptEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new C4ScriptSourceViewerConfiguration(getPreferenceStore(), colorManager,this));
		//setDocumentProvider(new ClonkDocumentProvider(this));
	}

	@Override
	protected void setDocumentProvider(IEditorInput input) {
		if (input instanceof ScriptWithStorageEditorInput) {
			setDocumentProvider(new ExternalScriptsDocumentProvider(this));
		} else {
			super.setDocumentProvider(input);
		}
	}
	
	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		
		// set partitioner (FIXME: remove again?)
		IDocument document = getDocumentProvider().getDocument(input);
		if (document.getDocumentPartitioner() == null) {
			IDocumentPartitioner partitioner =
				new FastPartitioner(
					new ClonkPartitionScanner(),
					ClonkPartitionScanner.C4S_PARTITIONS
				);
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
		}
	}
	
	@Override
	protected void editorSaved() {
		if (textChangeListener != null)
			textChangeListener.cancel();
		if (scriptBeingEdited() instanceof ScratchScript) {
			try {
				reparseWithDocumentContents(null, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
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

	private interface ICursorListener_Again extends KeyListener, MouseListener {}
	private final ICursorListener_Again showContentAssistAtKeyUpListener = new ICursorListener_Again() {

		@Override
		public void keyPressed(KeyEvent e) {}
		@Override
		public void mouseDoubleClick(MouseEvent e) {}
		@Override
		public void mouseDown(MouseEvent e) {}

		private void showContentAssistance() {
			// show parameter help
			ITextOperationTarget opTarget = (ITextOperationTarget) getSourceViewer();
			try {
				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart() == C4ScriptEditor.this)
					if (!getContentAssistant().isProposalPopupActive())
						if (opTarget.canDoOperation(ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION))
							opTarget.doOperation(ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
			} catch (NullPointerException nullP) {
				// might just be not that much of an issue
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			showContentAssistance();
		}

		@Override
		public void mouseUp(MouseEvent e) {
			showContentAssistance();
		}
	};
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		ScriptBase script = scriptBeingEdited();
		if (script != null && script.isEditable()) {
			textChangeListener = TextChangeListener.addTo(getDocumentProvider().getDocument(getEditorInput()), script, this);
		}
		getSourceViewer().getTextWidget().addMouseListener(showContentAssistAtKeyUpListener);
		getSourceViewer().getTextWidget().addKeyListener(showContentAssistAtKeyUpListener);
	}

	public void dispose() {
		if (textChangeListener != null) {
			textChangeListener.removeClient(this);
			textChangeListener = null;
		}
		colorManager.dispose();
		super.dispose();
	}
	
	private static final ResourceBundle messagesBundle = ResourceBundle.getBundle(ClonkCore.id("ui.editors.c4script.actionsBundle")); //$NON-NLS-1$
	
	protected void createActions() {
		super.createActions();

		IAction action;
		
		action = new TidyUpCodeAction(messagesBundle,"TidyUpCode.",this); //$NON-NLS-1$
		setAction(IClonkCommandIds.CONVERT_OLD_CODE_TO_NEW_CODE, action);
		
		action = new FindReferencesAction(messagesBundle,"FindReferences.",this); //$NON-NLS-1$
		setAction(IClonkCommandIds.FIND_REFERENCES, action);
		
		action = new RenameDeclarationAction(messagesBundle, "RenameDeclaration.", this); //$NON-NLS-1$
		setAction(IClonkCommandIds.RENAME_DECLARATION, action);
		
		action = new FindDuplicateAction(messagesBundle, "FindDuplicates.", this);
		setAction(IClonkCommandIds.FIND_DUPLICATES, action);
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#editorContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		if (scriptBeingEdited() != null) {
			if (scriptBeingEdited().isEditable()) {
				addAction(menu, IClonkCommandIds.CONVERT_OLD_CODE_TO_NEW_CODE);
				addAction(menu, IClonkCommandIds.RENAME_DECLARATION);
			}
			addAction(menu, IClonkCommandIds.FIND_REFERENCES);
			Function f = getFuncAtCursor();
			if (f != null) {
				addAction(menu, IClonkCommandIds.FIND_DUPLICATES);
			}
		}
	}
	
	private int cursorPos() {
		return ((TextSelection)getSelectionProvider().getSelection()).getOffset();
	}
	
	@Override
	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();
		
		// highlight active function
		Function f = getFuncAtCursor();
		boolean noHighlight = true;
		if (f != null) {
			this.setHighlightRange(f.getLocation().getOffset(), Math.min(
				f.getBody().getOffset()-f.getLocation().getOffset() + f.getBody().getLength() + (f.isOldStyle()?0:1),
				this.getDocumentProvider().getDocument(getEditorInput()).getLength()-f.getLocation().getOffset()
			), false);
			noHighlight = false;
		}
		if (noHighlight)
			this.resetHighlightRange();
		
		// inform auto edit strategy about cursor position change so it can delete its override regions
		getC4ScriptSourceViewerConfiguration().getAutoEditStrategy().handleCursorPositionChanged(
			cursorPos(), getDocumentProvider().getDocument(getEditorInput()));

	}

	private final C4ScriptSourceViewerConfiguration getC4ScriptSourceViewerConfiguration() {
		return (C4ScriptSourceViewerConfiguration)getSourceViewerConfiguration();
	}

	@Override
	public void completionProposalApplied(ClonkCompletionProposal proposal) {
		getC4ScriptSourceViewerConfiguration().getAutoEditStrategy().completionProposalApplied(proposal);
		super.completionProposalApplied(proposal);
	}

	/**
	 *  Created if there is no suitable script to get from somewhere else
	 *  can be considered a hack to make viewing (svn) revisions of a file work
	 */
	private ScriptBase scratchScript;
	
	public ScriptBase scriptBeingEdited() {
		if (getEditorInput() instanceof ScriptWithStorageEditorInput) {
			return ((ScriptWithStorageEditorInput)getEditorInput()).getScript();
		}
		IFile f;
		if ((f = Utilities.getEditingFile(this)) != null) {
			ScriptBase script = ScriptBase.get(f, true);
			if (script != null)
				return script;
		}

		if (scratchScript == null) {
			scratchScript = new ScratchScript(this);
			try {
				reparseWithDocumentContents(null, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return scratchScript;
	}

	public Function getFuncAt(int offset) {
		ScriptBase script = scriptBeingEdited();
		if (script != null) {
			Function f = script.funcAt(offset);
			return f;
		}
		return null;
	}
	
	public Function getFuncAtCursor() {
		return getFuncAt(cursorPos());
	}

	public C4ScriptParser reparseWithDocumentContents(IScriptParserListener exprListener, boolean onlyDeclarations) throws IOException, ParsingException {
		if (scriptBeingEdited() == null)
			return null;
		IDocument document = getDocumentProvider().getDocument(getEditorInput());
		return reparseWithDocumentContents(exprListener, onlyDeclarations, document, scriptBeingEdited(), new Runnable() {
			public void run() {
				refreshOutline();
				handleCursorPositionChanged();
			}
		});
	}

	private static C4ScriptParser reparseWithDocumentContents(
			IScriptParserListener exprListener,
			boolean onlyDeclarations, Object document,
			ScriptBase script,
			Runnable uiRefreshRunnable)
			throws ParsingException {
		C4ScriptParser parser;
		if (document instanceof IDocument) {
			parser = new C4ScriptParser(((IDocument)document).get(), script, script.getScriptFile());
		} else if (document instanceof IFile) {
			parser = new C4ScriptParser(StreamUtil.stringFromFile((IFile)document), script, (IFile)document);
		} else {
			throw new InvalidParameterException("document");
		}
		List<IStoredTypeInformation> storedLocalsTypeInformation = null;
		if (onlyDeclarations) {
			// when only parsing declarations store type information for variables declared in the script
			// and apply that information back to the variables after having reparsed so that type information is kept like it was (resulting from a full parse)
			storedLocalsTypeInformation = new LinkedList<IStoredTypeInformation>();
			for (Variable v : script.variables()) {
				IStoredTypeInformation info = v.getType() != null || v.getObjectType() != null ? AccessVar.createStoredTypeInformation(v) : null;
				if (info != null)
					storedLocalsTypeInformation.add(info);
			}
		}
		parser.setListener(exprListener);
		parser.clean();
		parser.parseDeclarations();
		if (!onlyDeclarations)
			parser.parseCodeOfFunctionsAndValidate();
		if (storedLocalsTypeInformation != null) {
			for (IStoredTypeInformation info : storedLocalsTypeInformation) {
				info.apply(false, parser);
			}
		}
		// make sure it's executed on the ui thread
		if (uiRefreshRunnable != null)
			Display.getDefault().asyncExec(uiRefreshRunnable);
		return parser;
	}
	
	@Override
	public ScriptBase getTopLevelDeclaration() {
		return scriptBeingEdited();
	}
	
	public static class FuncCallInfo {
		public CallFunc callFunc;
		public int parmIndex;
		public int parmsStart, parmsEnd;
		public FuncCallInfo(Function func, CallFunc callFunc, int parmIndex) {
			this.callFunc = callFunc;
			this.parmIndex = parmIndex;
			this.parmsStart = func.getBody().getStart()+callFunc.getParmsStart();
			this.parmsEnd = func.getBody().getStart()+callFunc.getParmsEnd();
		}
		public FuncCallInfo(Function func, CallFunc callFunc, ExprElm parm) {
			this(func, callFunc, parm != null ? callFunc.indexOfParm(parm) : 0);
		}
	}

	public FuncCallInfo getInnermostCallFuncExprParm(int offset) throws BadLocationException, ParsingException {
		Function f = this.getFuncAt(offset);
		if (f == null)
			return null;
		DeclarationLocator locator = new DeclarationLocator(this, getSourceViewer().getDocument(), new Region(offset, 0));
		ExprElm expr;

		// cursor somewhere between parm expressions... locate CallFunc and search
		int bodyStart = f.getBody().getStart();
		for (
			expr = locator.getExprAtRegion();
			expr != null;
			expr = expr.getParent()
		) {
			 if (expr instanceof CallFunc && offset-bodyStart >= ((CallFunc)expr).getParmsStart())
				 break;
		}
		if (expr != null) {
			CallFunc callFunc = (CallFunc) expr;
			ExprElm prev = null;
			for (ExprElm parm : callFunc.getParams()) {
				if (parm.getExprEnd() > offset) {
					if (prev == null)
						break;
					String docText = getSourceViewer().getDocument().get(bodyStart+prev.getExprEnd(), parm.getExprStart()-prev.getExprEnd());
					int commaIndex = docText.indexOf(',');
					return new FuncCallInfo(f, callFunc, offset >= bodyStart+prev.getExprEnd()+commaIndex ? parm : prev);
				}
				prev = parm;
			}
			return new FuncCallInfo(f, callFunc, prev);
		}
		return null;
	}

}
