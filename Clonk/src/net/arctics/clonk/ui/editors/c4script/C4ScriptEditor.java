package net.arctics.clonk.ui.editors.c4script;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4ScriptExprTree;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.ui.editors.ClonkCommandIds;
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
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.TextChangeListener;
import org.eclipse.swt.custom.TextChangedEvent;
import org.eclipse.swt.custom.TextChangingEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

public class C4ScriptEditor extends ClonkTextEditor {

	private ColorManager colorManager;
	public static final String ACTION_INDEX_CLONK_DIR = ClonkCore.id("indexClonkCommand");
	private static final String ENABLE_BRACKET_HIGHLIGHT = ClonkCore.id("enableBracketHighlighting");
	private static final String BRACKET_HIGHLIGHT_COLOR = ClonkCore.id("bracketHighlightColor");
	
	private DefaultCharacterPairMatcher fBracketMatcher = new DefaultCharacterPairMatcher(new char[] { '{', '}', '(', ')' });
	
	public C4ScriptEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new C4ScriptSourceViewerConfiguration(colorManager,this));
		setDocumentProvider(new ClonkDocumentProvider(this));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#configureSourceViewerDecorationSupport(org.eclipse.ui.texteditor.SourceViewerDecorationSupport)
	 */
	@Override
	protected void configureSourceViewerDecorationSupport(
			SourceViewerDecorationSupport support) {
		super.configureSourceViewerDecorationSupport(support);
		support.setCharacterPairMatcher(fBracketMatcher);
		support.setMatchingCharacterPainterPreferenceKeys(ENABLE_BRACKET_HIGHLIGHT, BRACKET_HIGHLIGHT_COLOR);
		getPreferenceStore().setValue(ENABLE_BRACKET_HIGHLIGHT, true);
		PreferenceConverter.setValue(getPreferenceStore(), BRACKET_HIGHLIGHT_COLOR, new RGB(0x33,0x33,0xAA));
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
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
		getSourceViewer().getTextWidget().getContent().addTextChangeListener(new TextChangeListener() {
			
			private Timer reparseTimer;

			public void textChanged(TextChangedEvent event) {
				markScriptAsDirty();
				if (getDocumentProvider().getDocument(getEditorInput()).getLength() > 20 * 1024)
					return;
				reparseTimer = new Timer("ReparseTimer");
				reparseTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						try {
							try {
								reparseWithDocumentContents(null, true);
							} finally {
								if (reparseTimer != null) {
									reparseTimer.cancel();
									reparseTimer = null;
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}, 2000);
			}

			public void textChanging(TextChangingEvent event) {
				// TODO Auto-generated method stub
				
			}

			public void textSet(TextChangedEvent event) {
				// TODO Auto-generated method stub
				
			}
			
		});
	}

	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
	
	protected void createActions() {
		super.createActions();
		ResourceBundle messagesBundle = ResourceBundle.getBundle("net.arctics.clonk.ui.editors.c4script.Messages"); //$NON-NLS-1$
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
		setAction(ClonkCommandIds.CONVERT_OLD_CODE_TO_NEW_CODE, action);
		
		action = new FindReferencesAction(messagesBundle,"FindReferences.",this); //$NON-NLS-1$
		setAction(ClonkCommandIds.FIND_REFERENCES, action);
		
		action = new RenameFieldAction(messagesBundle, "RenameField.", this);
		setAction(ClonkCommandIds.RENAME_FIELD, action);
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#createSourceViewer(org.eclipse.swt.widgets.Composite, org.eclipse.jface.text.source.IVerticalRuler, int)
	 */
	@Override
	protected ISourceViewer createSourceViewer(Composite parent,
			IVerticalRuler ruler, int styles) {
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
			addAction(menu, ClonkCommandIds.CONVERT_OLD_CODE_TO_NEW_CODE);
			addAction(menu, ClonkCommandIds.RENAME_FIELD);
		}
		addAction(menu, ClonkCommandIds.FIND_REFERENCES);
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
	}

	public C4Function getFuncAtCursor() {
		C4ScriptBase script = Utilities.getScriptForEditor(this);
		if (script != null) {
			TextSelection sel = (TextSelection) getSelectionProvider().getSelection();
			C4Function f = script.funcAt(sel.getOffset());
			return f;
		}
		return null;
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
	public C4Declaration getTopLevelDeclaration() {
		return Utilities.getScriptForEditor(this);
	}

}
