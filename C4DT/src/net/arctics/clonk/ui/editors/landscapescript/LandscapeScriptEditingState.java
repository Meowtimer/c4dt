package net.arctics.clonk.ui.editors.landscapescript;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.landscapescript.LandscapeScript;
import net.arctics.clonk.landscapescript.LandscapeScriptLexer;
import net.arctics.clonk.landscapescript.LandscapeScriptParser;
import net.arctics.clonk.landscapescript.Overlay;
import net.arctics.clonk.landscapescript.OverlayBase;
import net.arctics.clonk.ui.editors.CStylePartitionScanner;
import net.arctics.clonk.ui.editors.EntityHyperlink;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.ScriptCommentScanner;
import net.arctics.clonk.ui.editors.StructureEditingState;
import net.arctics.clonk.ui.navigator.ClonkPreviewView;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class LandscapeScriptEditingState extends StructureEditingState<LandscapeScriptEditor, LandscapeScript> {
	public LandscapeScriptEditingState(IPreferenceStore store) { super(store); }
	private boolean parsed;
	public void silentReparse() {
		final IFile file = structure().file();
		structure().setFile(null);
		try {
			reparse();
		}
		finally {
			structure().setFile(file);
		}
	}
	public void reparse() {
		if (!parsed) {
			final LandscapeScript script = structure();
			if (script == null)
				return;
			final String documentText = document.get();
			final CharStream charStream = new ANTLRStringStream(documentText);
			final LandscapeScriptLexer lexer = new LandscapeScriptLexer(charStream);
			final CommonTokenStream tokenStream = new CommonTokenStream();
			tokenStream.setTokenSource(lexer);
			final LandscapeScriptParser parser = new LandscapeScriptParser(structure(), tokenStream);
			structure().clear();
			parser.parse();
			parsed = true;
			final IFile file = script.file();
			try {
				for (final IWorkbenchWindow w : PlatformUI.getWorkbench().getWorkbenchWindows()) {
					final ClonkPreviewView view = (ClonkPreviewView) w.getActivePage().findView(ClonkPreviewView.ID);
					if (view != null) {
						final IStructuredSelection sel = as(view.getSelectionOfInterest(), IStructuredSelection.class);
						if (
							script.engine() != null && script.engine().settings().supportsEmbeddedUtilities &&
							sel != null && sel.getFirstElement().equals(file)
						)
							view.schedulePreviewUpdaterJob();
					}
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}
	public class LandscapeHyperlinkDetector implements IHyperlinkDetector {
		@Override
		public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
			final OverlayBase overlay = structure().overlayAt(region.getOffset());
			// link to template (linking other things does not seem to make much sense)
			if (overlay instanceof Overlay && ((Overlay)overlay).template() != null && region.getOffset()-overlay.start() < ((Overlay) overlay).template().name().length())
				return new IHyperlink[] {new EntityHyperlink(new Region(overlay.start(), ((Overlay) overlay).template().name().length()), ((Overlay) overlay).template())};
			return null;
		}
	}
	private final LandscapeScriptCodeScanner scanner = new LandscapeScriptCodeScanner(ColorManager.INSTANCE);
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		final PresentationReconciler reconciler = new PresentationReconciler();

		final ScriptCommentScanner commentScanner = new ScriptCommentScanner(getColorManager(), "COMMENT");

		DefaultDamagerRepairer dr =
			new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, CStylePartitionScanner.CODEBODY);
		reconciler.setRepairer(dr, CStylePartitionScanner.CODEBODY);

		dr = new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, CStylePartitionScanner.STRING);
		reconciler.setRepairer(dr, CStylePartitionScanner.STRING);

		dr = new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(commentScanner);
		reconciler.setDamager(dr, CStylePartitionScanner.COMMENT);
		reconciler.setRepairer(dr, CStylePartitionScanner.COMMENT);

		return reconciler;
	}
	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		try {
			return new IHyperlinkDetector[] {
				new LandscapeHyperlinkDetector()
			};
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	@Override
	public ContentAssistant createAssistant() {
		final ContentAssistant assistant = new ContentAssistant();
		final LandscapeScriptCompletionProcessor processor = new LandscapeScriptCompletionProcessor(this);
		assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setStatusLineVisible(true);
		assistant.setStatusMessage(String.format(Messages.LandscapeScriptSourceViewerConfiguration_Proposals, structure().file().getName()));
		assistant.enablePrefixCompletion(false);
		assistant.enableAutoInsert(true);
		assistant.enableAutoActivation(true);
		assistant.enableColoredLabels(true);
		return assistant;
	}
}
