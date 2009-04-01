package net.arctics.clonk.ui.editors.c4script;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4ObjectIntern;
import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.parser.C4ScriptExprTree;
import net.arctics.clonk.parser.C4ScriptIntern;
import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.C4ScriptParser.ParsingException;
import net.arctics.clonk.ui.editors.actions.c4script.ConvertOldCodeToNewCodeAction;
import net.arctics.clonk.ui.editors.actions.c4script.FindReferencesAction;
import net.arctics.clonk.ui.editors.actions.c4script.OpenDeclarationAction;
import net.arctics.clonk.ui.editors.actions.c4script.RenameFieldAction;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class C4ScriptEditor extends TextEditor {

	private ColorManager colorManager;
	private ClonkContentOutlinePage outlinePage;
	public static final String ACTION_INDEX_CLONK_DIR = "net.arctics.clonk.indexClonkCommand";
	private static final String ENABLE_BRACKET_HIGHLIGHT = ClonkCore.PLUGIN_ID + ".enableBracketHighlighting";
	private static final String BRACKET_HIGHLIGHT_COLOR = ClonkCore.PLUGIN_ID + ".bracketHighlightColor";
	private DefaultCharacterPairMatcher fBracketMatcher = new DefaultCharacterPairMatcher(new char[] { '{', '}', '(', ')' });
	private ShowInAdapter showInAdapter;
	
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
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#configureSourceViewerDecorationSupport(org.eclipse.ui.texteditor.SourceViewerDecorationSupport)
	 */
	@Override
	protected void configureSourceViewerDecorationSupport(
			SourceViewerDecorationSupport support) {
		support.setCharacterPairMatcher(fBracketMatcher);
		support.setMatchingCharacterPainterPreferenceKeys(ENABLE_BRACKET_HIGHLIGHT, BRACKET_HIGHLIGHT_COLOR);
		getPreferenceStore().setValue(ENABLE_BRACKET_HIGHLIGHT, true);
		PreferenceConverter.setValue(getPreferenceStore(), BRACKET_HIGHLIGHT_COLOR, new RGB(0x33,0x33,0xAA));
		super.configureSourceViewerDecorationSupport(support);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		IResource res = (IResource) getEditorInput().getAdapter(IResource.class);
		if (res != null) {
			setPartName(res.getParent().getName() + "/" + res.getName());
		}
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

	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
	
	protected void createActions() {
		super.createActions();
		ResourceBundle messagesBundle = ResourceBundle.getBundle("net.arctics.clonk.ui.editors.c4script.Messages"); //$NON-NLS-1$
		
		IAction action = new ContentAssistAction(messagesBundle,"ClonkContentAssist.",this); //$NON-NLS-1$
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, action);
		
		action = new ContentAssistAction(messagesBundle,"ClonkContentAssist.",this); //$NON-NLS-1$
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.SHOW_INFORMATION);
		setAction(ITextEditorActionDefinitionIds.SHOW_INFORMATION, action);
		
		action = new ContentAssistAction(messagesBundle,"ClonkContentAssist.",this); //$NON-NLS-1$
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
		setAction(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION, action);
		
		action = new ConvertOldCodeToNewCodeAction(messagesBundle,"ConvertOldCodeToNewCode.",this); //$NON-NLS-1$
		setAction(ClonkCommandIds.CONVERT_OLD_CODE_TO_NEW_CODE, action);
		
		action = new OpenDeclarationAction(messagesBundle,"OpenDeclaration.",this); //$NON-NLS-1$
		setAction(ClonkCommandIds.OPEN_DECLARATION, action);
		
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
		
		fAnnotationAccess= getAnnotationAccess();
		fOverviewRuler= createOverviewRuler(getSharedColors());

		ISourceViewer viewer= new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
		
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
		if (!(getEditorInput() instanceof ScriptWithStorageEditorInput))
			addAction(menu, ClonkCommandIds.CONVERT_OLD_CODE_TO_NEW_CODE);
		addAction(menu, ClonkCommandIds.OPEN_DECLARATION);
		addAction(menu, ClonkCommandIds.FIND_REFERENCES);
		addAction(menu, ClonkCommandIds.RENAME_FIELD);
	}

	public void selectAndReveal(SourceLocation location) {
		this.selectAndReveal(location.getStart(), location.getEnd() - location.getStart());
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

	public void refreshOutline() {
		if (outlinePage != null) // don't start lazy loading of outlinePage
			getOutlinePage().refresh();
	}

	@Override
	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();
		C4ScriptBase script = Utilities.getScriptForEditor(this);
		boolean noHighlight = true;
		if (script != null) {
			TextSelection sel = (TextSelection) getSelectionProvider().getSelection();
			C4Function f = script.funcAt(sel.getOffset());
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

	public C4ScriptParser reparseWithDocumentContents(C4ScriptExprTree.IExpressionListener exprListener, boolean onlyDeclarations) throws IOException, ParsingException {
		IDocument document = getDocumentProvider().getDocument(getEditorInput());
		byte[] documentBytes = document.get().getBytes();
		InputStream scriptStream = new ByteArrayInputStream(documentBytes);
		C4ScriptParser parser = new C4ScriptParser(scriptStream, documentBytes.length, Utilities.getScriptForEditor(this));
		scriptStream.close();
		parser.setExpressionListener(exprListener);
		parser.clean();
		parser.parseDeclarations();
		if (!onlyDeclarations)
			parser.parseCodeOfFunctions();
		refreshOutline();
		return parser;
	}
	
	public static IEditorPart openDeclaration(C4Declaration target) throws PartInitException, IOException, ParsingException {
		return openDeclaration(target, true);
	}
	
	public static IEditorPart openDeclaration(C4Declaration target, boolean activate) throws PartInitException, IOException, ParsingException {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchPage workbenchPage = workbench.getActiveWorkbenchWindow().getActivePage();
		C4ScriptBase script = target instanceof C4ScriptBase ? (C4ScriptBase)target : target.getScript();
		if (script != null) {
			if (script instanceof C4ObjectIntern || script instanceof C4ScriptIntern) {
				IFile scriptFile = (IFile) script.getScriptFile();
				if (scriptFile != null) {
					IEditorPart editor = IDE.openEditor(workbenchPage, scriptFile, "clonk.editors.C4ScriptEditor", activate);
					C4ScriptEditor scriptEditor = (C4ScriptEditor)editor;						
					if (target != script) {
						scriptEditor.reparseWithDocumentContents(null, false);
						target = target.latestVersion();
						if (target != null)
							scriptEditor.selectAndReveal(target.getLocation());
					}
					return scriptEditor;
				} else {
					IFile defCore = ((C4ObjectIntern)script).getDefCoreFile();
					if (defCore != null)
						return IDE.openEditor(workbenchPage, defCore, activate);
				}
			}
			else if (script.getScriptFile() instanceof IStorage) {
				if (script != ClonkCore.getDefault().ENGINE_OBJECT) {
					IEditorPart editor = workbenchPage.openEditor(new ScriptWithStorageEditorInput(script), "clonk.editors.C4ScriptEditor");
					C4ScriptEditor scriptEditor = (C4ScriptEditor)editor;
					if (target != script)
						scriptEditor.selectAndReveal(target.getLocation());
					return scriptEditor;
				}
			}
		}
		return null;
	}

}
