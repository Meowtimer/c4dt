package net.arctics.clonk.ui.editors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ResourceBundle;

import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4ScriptExprTree;
import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.CompilerException;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.ui.editors.actions.ConvertOldCodeToNewCodeAction;
import net.arctics.clonk.ui.editors.actions.OpenDeclarationAction;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

public class C4ScriptEditor extends AbstractDecoratedTextEditor {

	private ColorManager colorManager;
	private ClonkContentOutlinePage outlinePage;
	public static final String ACTION_INDEX_CLONK_DIR = "net.arctics.clonk.indexClonkCommand";
	private DefaultCharacterPairMatcher fBracketMatcher = new DefaultCharacterPairMatcher(new char[] { '{', '}' });
	
	
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
		super.configureSourceViewerDecorationSupport(support);
		support.setCharacterPairMatcher(fBracketMatcher);
//		support.setMatchingCharacterPainterPreferenceKeys(I
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#getPartName()
	 */
	public String getPartName() {
//		String part = super.getPartName();
		IResource res = (IResource) getEditorInput().getAdapter(IResource.class);
		if (res == null) return super.getPartName();
		return res.getParent().getName() + "/" + super.getPartName();
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
		return super.getAdapter(adapter);
	}

	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
	
	protected void createActions() {
		super.createActions();
		ResourceBundle messagesBundle = ResourceBundle.getBundle("net.arctics.clonk.ui.editors.Messages"); //$NON-NLS-1$
		
		IAction action= new ContentAssistAction(messagesBundle,"ClonkContentAssist.",this); //$NON-NLS-1$
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
		
		action = new OpenDeclarationAction(messagesBundle,"OpenDeclarationAction.",this); //$NON-NLS-1$
		action.setActionDefinitionId(ClonkCommandIds.OPEN_DECLARATION);
		setAction(ClonkCommandIds.OPEN_DECLARATION, action);
		
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
		addAction(menu, ClonkCommandIds.CONVERT_OLD_CODE_TO_NEW_CODE);
		addAction(menu, ClonkCommandIds.OPEN_DECLARATION);
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
		outlinePage.refresh();
	}

	public C4ScriptParser reparseWithDocumentContents(C4ScriptExprTree.IExpressionListener exprListener, boolean onlyDeclarations) throws CompilerException {
		IDocument document = getDocumentProvider().getDocument(getEditorInput());
		byte[] documentBytes = document.get().getBytes();
		InputStream scriptStream = new ByteArrayInputStream(documentBytes);
		C4ScriptParser parser = new C4ScriptParser(scriptStream, documentBytes.length, Utilities.getObjectForEditor(this));
		parser.setExpressionListener(exprListener);
		parser.clean();
		parser.parseDeclarations();
		if (!onlyDeclarations)
			parser.parseCodeOfFunctions();
		refreshOutline();
		return parser;
	}

}
