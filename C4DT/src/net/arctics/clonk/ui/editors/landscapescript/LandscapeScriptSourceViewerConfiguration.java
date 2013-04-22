package net.arctics.clonk.ui.editors.landscapescript;

import net.arctics.clonk.landscapescript.Overlay;
import net.arctics.clonk.landscapescript.OverlayBase;
import net.arctics.clonk.ui.editors.CStylePartitionScanner;
import net.arctics.clonk.ui.editors.ClonkHyperlink;
import net.arctics.clonk.ui.editors.ClonkSourceViewerConfiguration;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.ScriptCommentScanner;
import net.arctics.clonk.util.Utilities;

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

public class LandscapeScriptSourceViewerConfiguration extends ClonkSourceViewerConfiguration<LandscapeScriptEditor> {

	public class LandscapeHyperlinkDetector implements IHyperlinkDetector {
		@Override
		public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
				IRegion region, boolean canShowMultipleHyperlinks) {
			OverlayBase overlay = editor().script().overlayAt(region.getOffset());
			// link to template (linking other things does not seem to make much sense)
			if (overlay instanceof Overlay && ((Overlay)overlay).template() != null && region.getOffset()-overlay.start() < ((Overlay) overlay).template().name().length())
				return new IHyperlink[] {new ClonkHyperlink(new Region(overlay.start(), ((Overlay) overlay).template().name().length()), ((Overlay) overlay).template())};
			return null;
		}
	}

	private final LandscapeScriptCodeScanner scanner = new LandscapeScriptCodeScanner(ColorManager.instance());

	public LandscapeScriptSourceViewerConfiguration(IPreferenceStore store, ColorManager colorManager, LandscapeScriptEditor textEditor) {
		super(store, colorManager, textEditor);
	}
	
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		
		ScriptCommentScanner commentScanner = new ScriptCommentScanner(getColorManager(), "COMMENT");
		
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
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
//		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
//		assistant.setContentAssistProcessor(new CodeBodyCompletionProcessor(getEditor(),assistant), ClonkPartitionScanner.C4S_CODEBODY);
		LandscapeScriptCompletionProcessor processor = new LandscapeScriptCompletionProcessor(editor(), assistant);
		assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		assistant.install(sourceViewer);
		
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		

		//assistant.setRepeatedInvocationMode(true);
		// key sequence is set in constructor of ClonkCompletionProcessor
		
		assistant.setStatusLineVisible(true);
		assistant.setStatusMessage(String.format(Messages.LandscapeScriptSourceViewerConfiguration_Proposals, Utilities.fileEditedBy(editor()).getName()));
		
		assistant.enablePrefixCompletion(false);
		assistant.enableAutoInsert(true);
		assistant.enableAutoActivation(true);
		
		assistant.enableColoredLabels(true);
		
		return assistant;
	}

}
