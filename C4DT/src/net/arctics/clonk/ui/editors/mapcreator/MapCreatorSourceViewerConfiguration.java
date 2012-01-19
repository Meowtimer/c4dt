package net.arctics.clonk.ui.editors.mapcreator;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;

import net.arctics.clonk.parser.mapcreator.MapOverlay;
import net.arctics.clonk.parser.mapcreator.MapOverlayBase;
import net.arctics.clonk.ui.editors.ClonkHyperlink;
import net.arctics.clonk.ui.editors.ClonkPartitionScanner;
import net.arctics.clonk.ui.editors.ClonkSourceViewerConfiguration;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.ClonkColorConstants;
import net.arctics.clonk.ui.editors.ScriptCommentScanner;
import net.arctics.clonk.util.Utilities;

public class MapCreatorSourceViewerConfiguration extends ClonkSourceViewerConfiguration<MapCreatorEditor> {

	public class MapCreatorHyperlinkDetector implements IHyperlinkDetector {

		@Override
		public IHyperlink[] detectHyperlinks(ITextViewer textViewer,
				IRegion region, boolean canShowMultipleHyperlinks) {
			MapOverlayBase overlay = getEditor().getMapCreator().overlayAt(region.getOffset());
			// link to template (linking other things does not seem to make much sense)
			if (overlay instanceof MapOverlay && ((MapOverlay)overlay).template() != null && region.getOffset()-overlay.getLocation().getStart() < ((MapOverlay) overlay).template().name().length())
				return new IHyperlink[] {new ClonkHyperlink(new Region(overlay.getLocation().getOffset(), ((MapOverlay) overlay).template().name().length()), ((MapOverlay) overlay).template())};
			return null;
		}

	}

	private RuleBasedScanner scanner;
	private ScriptCommentScanner commentScanner;

	public MapCreatorSourceViewerConfiguration(IPreferenceStore store, ColorManager colorManager, MapCreatorEditor textEditor) {
		super(store, colorManager, textEditor);
	}
	
	protected ITokenScanner getClonkScanner() {
		if (scanner == null) {
			scanner = new MapCreatorCodeScanner(getColorManager());
			scanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						getColorManager().getColor(ClonkColorConstants.getColor("DEFAULT"))))); //$NON-NLS-1$
		}
		return scanner;
	}
	
	protected ScriptCommentScanner getClonkCommentScanner() {
		if (commentScanner == null) {
			commentScanner = new ScriptCommentScanner(getColorManager());
			commentScanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						getColorManager().getColor(ClonkColorConstants.getColor("COMMENT"))))); //$NON-NLS-1$
		}
		return commentScanner;
	}
	
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		
		DefaultDamagerRepairer dr =
			new DefaultDamagerRepairer(getClonkScanner());
		reconciler.setDamager(dr, ClonkPartitionScanner.C4S_CODEBODY);
		reconciler.setRepairer(dr, ClonkPartitionScanner.C4S_CODEBODY);
		
		dr = new DefaultDamagerRepairer(getClonkScanner());
		reconciler.setDamager(dr, ClonkPartitionScanner.C4S_STRING);
		reconciler.setRepairer(dr, ClonkPartitionScanner.C4S_STRING);
		
		dr = new DefaultDamagerRepairer(getClonkScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		
		dr = new DefaultDamagerRepairer(getClonkCommentScanner());
		reconciler.setDamager(dr, ClonkPartitionScanner.C4S_COMMENT);
		reconciler.setRepairer(dr, ClonkPartitionScanner.C4S_COMMENT);
		
//		NonRuleBasedDamagerRepairer ndr =
//			new NonRuleBasedDamagerRepairer(
//				new TextAttribute(
//					colorManager.getColor(IClonkColorConstants.getColor("COMMENT"))));
//		
//		reconciler.setDamager(ndr, ClonkPartitionScanner.C4S_COMMENT);
//		reconciler.setRepairer(ndr, ClonkPartitionScanner.C4S_COMMENT);
		
		return reconciler;
	}
	
	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		try {
			return new IHyperlinkDetector[] {
				new MapCreatorHyperlinkDetector()
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
		MapCreatorCompletionProcessor processor = new MapCreatorCompletionProcessor(getEditor());
		assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		assistant.install(sourceViewer);
		
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		

		//assistant.setRepeatedInvocationMode(true);
		// key sequence is set in constructor of ClonkCompletionProcessor
		
		assistant.setStatusLineVisible(true);
		assistant.setStatusMessage(String.format(Messages.MapCreatorSourceViewerConfiguration_Proposals, Utilities.fileBeingEditedBy(getEditor()).getName()));
		
		assistant.enablePrefixCompletion(false);
		assistant.enableAutoInsert(true);
		assistant.enableAutoActivation(true);
		
		assistant.enableColoredLabels(true);
		
		return assistant;
	}

}
