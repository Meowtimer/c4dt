package net.arctics.clonk.ui.editors.c4script;

import java.io.ByteArrayInputStream;


import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.CallFunc;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree.ExprElm;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.ui.editors.IClonkCommandIds;
import net.arctics.clonk.ui.editors.ClonkDocumentProvider;
import net.arctics.clonk.ui.editors.ClonkTextEditor;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.actions.c4script.ConvertOldCodeToNewCodeAction;
import net.arctics.clonk.ui.editors.actions.c4script.FindReferencesAction;
import net.arctics.clonk.ui.editors.actions.c4script.RenameFieldAction;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

public class C4ScriptEditor extends ClonkTextEditor {

	// Helper class that takes care of triggering a timed reparsing when the document is changed and such
	// it tries to only fire a reparse when necessary (ie not when editing inside of a function)
	private final class TextChangeListener implements IDocumentListener {
		
		private Timer reparseTimer = new Timer("ReparseTimer");
		private TimerTask reparseTask;

		public void documentAboutToBeChanged(DocumentEvent event) {
		}

		private void addToLocation(SourceLocation location, int offset, int add) {
			if (location.getStart() >= offset)
				location.setStart(location.getStart()+add);
			if (location.getEnd() >= offset)
				location.setEnd(location.getEnd()+add);
		}

		private void adjustDec(C4Declaration declaration, int offset, int add) {
			addToLocation(declaration.getLocation(), offset, add);
			if (declaration instanceof C4Function) {
				C4Function f = (C4Function) declaration;
				addToLocation(f.getBody(), offset, add);
				for (C4Declaration v : f.allSubDeclarations()) {
					addToLocation(v.getLocation(), offset, add);
				}
			}
		}

		public void documentChanged(DocumentEvent event) {
			markScriptAsDirty();
			C4Function f = getFuncAt(event.getOffset());
			if (f != null && !f.isOldStyle()) {
				adjustDeclarationLocations(event);
			} else {
				// only schedule reparsing when editing outside of existing function
				scheduleReparsing();
			}
		}

		private void adjustDeclarationLocations(DocumentEvent event) {
			if (event.getLength() == 0) {
				// text was added
				for (C4Declaration dec : scriptBeingEdited().allSubDeclarations()) {
					adjustDec(dec, event.getOffset(), event.getText().length());
				}
			}
			else if (event.getLength() == 0) {
				// text was removed
				for (C4Declaration dec : scriptBeingEdited().allSubDeclarations()) {
					adjustDec(dec, event.getOffset(), event.getText().length());
				}
			}
			else {
				String newText = event.getText();
				int replLength = event.getLength();
				int offset = event.getOffset();
				int diff = newText.length() - replLength;
				// mixed
				for (C4Declaration dec : scriptBeingEdited().allSubDeclarations()) {
					if (dec.getLocation().getStart() >= offset + replLength)
						adjustDec(dec, offset, diff);
					else if (dec instanceof C4Function) {
						// inside function: expand end location
						C4Function func = (C4Function) dec;
						if (offset >= func.getBody().getStart() && offset+replLength < func.getBody().getEnd()) {
							func.getBody().setEnd(func.getBody().getEnd()+diff);
						}
					}
				}
			}
		}
		
		public void cancel() {
			if (reparseTask != null) {
				reparseTask.cancel();
				reparseTask = null;
			}
		}

		private void scheduleReparsing() {
			cancel();
			reparseTimer.schedule(reparseTask = new TimerTask() {
				@Override
				public void run() {
					try {
						try {
							reparseWithDocumentContents(null, true);
						} finally {
							cancel();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, 2000);
		}

		public void dispose() {
			cancel();
			reparseTimer.cancel();
			reparseTimer = null;
		}
	}

	private ColorManager colorManager;
	private static final String ENABLE_BRACKET_HIGHLIGHT = ClonkCore.id("enableBracketHighlighting");
	private static final String BRACKET_HIGHLIGHT_COLOR = ClonkCore.id("bracketHighlightColor");
	
	private DefaultCharacterPairMatcher fBracketMatcher = new DefaultCharacterPairMatcher(new char[] { '{', '}', '(', ')' });
	private TextChangeListener textChangeListener;
	
	public C4ScriptEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new C4ScriptSourceViewerConfiguration(colorManager,this));
		setDocumentProvider(new ClonkDocumentProvider(this));
	}
	
	@Override
	protected void editorSaved() {
		textChangeListener.cancel();
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

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		IResource res = (IResource) getEditorInput().getAdapter(IResource.class);
		if (res != null) {
			// name of script file not very descriptive (Script.c)
			setPartName(res.getParent().getName() + "/" + res.getName());
		}
	}
	
	private void markScriptAsDirty() {
		Utilities.getScriptForEditor(this).setDirty(true);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		getDocumentProvider().getDocument(getEditorInput()).addDocumentListener(textChangeListener = new TextChangeListener());
	}

	public void dispose() {
		getDocumentProvider().getDocument(getEditorInput()).removeDocumentListener(textChangeListener);
		textChangeListener.dispose();
		colorManager.dispose();
		super.dispose();
	}
	
	protected void createActions() {
		super.createActions();
		ResourceBundle messagesBundle = ResourceBundle.getBundle(ClonkCore.id("ui.editors.c4script.Messages")); //$NON-NLS-1$
//		
		IAction action;
		
//		IAction action = new ContentAssistAction(messagesBundle,"ClonkContentAssist.",this); //$NON-NLS-1$
//		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
//		setAction(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, action);
//		
//		action = new ContentAssistAction(messagesBundle,"ClonkContentAssist.",this); //$NON-NLS-1$
//		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SHOW_INFORMATION);
//		setAction(ITextEditorActionDefinitionIds.SHOW_INFORMATION, action);
//		
//		action = new ContentAssistAction(messagesBundle,"ClonkContentAssist.",this); //$NON-NLS-1$
//		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
//		setAction(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION, action);
		
		action = new ConvertOldCodeToNewCodeAction(messagesBundle,"ConvertOldCodeToNewCode.",this); //$NON-NLS-1$
		setAction(IClonkCommandIds.CONVERT_OLD_CODE_TO_NEW_CODE, action);
		
		action = new FindReferencesAction(messagesBundle,"FindReferences.",this); //$NON-NLS-1$
		setAction(IClonkCommandIds.FIND_REFERENCES, action);
		
		action = new RenameFieldAction(messagesBundle, "RenameField.", this);
		setAction(IClonkCommandIds.RENAME_FIELD, action);
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#createSourceViewer(org.eclipse.swt.widgets.Composite, org.eclipse.jface.text.source.IVerticalRuler, int)
	 */
	@Override
	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
//		return super.createSourceViewer(parent, ruler, styles);
		
		fAnnotationAccess = getAnnotationAccess();
		fOverviewRuler = createOverviewRuler(getSharedColors());

		ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
		
		// ensure decoration support has been created and configured.
		getSourceViewerDecorationSupport(viewer);
		
		return viewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#editorContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		if (Utilities.getScriptForEditor(this).isEditable()) {
			addAction(menu, IClonkCommandIds.CONVERT_OLD_CODE_TO_NEW_CODE);
			addAction(menu, IClonkCommandIds.RENAME_FIELD);
		}
		addAction(menu, IClonkCommandIds.FIND_REFERENCES);
	}

	@Override
	protected void doSetSelection(ISelection selection) {
		super.doSetSelection(selection);
//		C4Object obj = Utilities.getObjectForEditor(this);
//		if (obj != null) {
//			C4Function func = obj.funcAt((ITextSelection)selection);
//			getOutlinePage().select(func);
//		}
	}

	@Override
	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();
		boolean noHighlight = true;
		C4Function f = getFuncAtCursor();
		if (f != null) {
			if (f != null) {
				this.setHighlightRange(f.getLocation().getOffset(), Math.min(
					f.getBody().getOffset()-f.getLocation().getOffset() + f.getBody().getLength() + (f.isOldStyle()?0:1),
					this.getDocumentProvider().getDocument(getEditorInput()).getLength()-f.getLocation().getOffset()
				), false);
				noHighlight = false;
			} 
		}
		if (noHighlight)
			this.resetHighlightRange();
//		try {
//			CallFunc callFunc = getInnermostCallFuncExpr(((TextSelection)getSelectionProvider().getSelection()).getOffset());
//			if (callFunc != null)
//				((ContentAssistant)getSourceViewerConfiguration().getContentAssistant(getSourceViewer())).showContextInformation();
//        } catch (Exception e) {
//	        e.printStackTrace();
//        }
	}

	public C4ScriptBase scriptBeingEdited() {
		return Utilities.getScriptForEditor(this);
	}
	
	public C4Function getFuncAt(int offset) {
		C4ScriptBase script = scriptBeingEdited();
		if (script != null) {
			C4Function f = script.funcAt(offset);
			return f;
		}
		return null;
	}
	
	public C4Function getFuncAtCursor() {
		return getFuncAt(((TextSelection)getSelectionProvider().getSelection()).getOffset());
	}

	public C4ScriptParser reparseWithDocumentContents(C4ScriptExprTree.IExpressionListener exprListener, boolean onlyDeclarations) throws IOException, ParsingException {
		IDocument document = getDocumentProvider().getDocument(getEditorInput());
		byte[] documentBytes = document.get().getBytes();
		InputStream scriptStream = new ByteArrayInputStream(documentBytes);
		C4ScriptParser parser = new C4ScriptParser(scriptStream, Utilities.getScriptForEditor(this));
		scriptStream.close();
		parser.setExpressionListener(exprListener);
		parser.clean();
		parser.parseDeclarations();
		if (!onlyDeclarations)
			parser.parseCodeOfFunctions();
		// make sure it's executed on the ui thread
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				refreshOutline();
			}
		});
		return parser;
	}
	
	@Override
	public C4ScriptBase getTopLevelDeclaration() {
		return scriptBeingEdited();
	}
	
	public CallFunc getInnermostCallFuncExpr(int offset) throws BadLocationException, ParsingException {
		DeclarationLocator locator = new DeclarationLocator(this, getSourceViewer().getDocument(), new Region(offset, 0));
		ExprElm expr;
		for (expr = locator.getExprAtRegion(); expr != null && !(expr instanceof CallFunc); expr = expr.getParent());
		if (expr != null) {
			CallFunc callFunc = (CallFunc) expr;
			if (offset-this.getFuncAt(offset).getBody().getOffset() < callFunc.getParmsStart())
				return null;
			return callFunc;
		}
		return null;
	}

}
