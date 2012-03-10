package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.ui.editors.ClonkColorConstants;
import net.arctics.clonk.ui.editors.ClonkContentAssistant;
import net.arctics.clonk.ui.editors.ClonkHyperlink;
import net.arctics.clonk.ui.editors.ClonkPartitionScanner;
import net.arctics.clonk.ui.editors.ClonkSourceViewerConfiguration;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.ScriptCommentScanner;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Shell;

public class C4ScriptSourceViewerConfiguration extends ClonkSourceViewerConfiguration<C4ScriptEditor> {
	
	private class C4ScriptHyperlinkDetector implements IHyperlinkDetector {
		@Override
		public IHyperlink[] detectHyperlinks(ITextViewer viewer, IRegion region, boolean canShowMultipleHyperlinks) {
			try {
				EntityLocator locator = new EntityLocator(editor(), viewer.getDocument(),region);
				if (locator.entity() != null)
					return new IHyperlink[] {
						new ClonkHyperlink(locator.expressionRegion(), locator.entity())
					};
				else if (locator.potentialEntities() != null)
					return new IHyperlink[] {
						new ClonkHyperlink(locator.expressionRegion(), locator.potentialEntities())
					};
				return null;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	private C4ScriptCodeScanner scanner;
	private ScriptCommentScanner commentScanner;
	private ITextDoubleClickStrategy doubleClickStrategy;

	public C4ScriptSourceViewerConfiguration(IPreferenceStore store, ColorManager colorManager, C4ScriptEditor textEditor) {
		super(store, colorManager, textEditor);
	}
	
	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return ClonkPartitionScanner.C4S_PARTITIONS;
	}
	
	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new C4ScriptDoubleClickStrategy(this);
		return doubleClickStrategy;
	}

	protected C4ScriptCodeScanner clonkScanner() {
		if (scanner == null) {
			scanner = new C4ScriptCodeScanner(getColorManager(), editor().scriptBeingEdited().engine());
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

	private ClonkContentAssistant assistant;
	
	@Override
	public ClonkContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		if (assistant != null)
			return assistant;
		
		assistant = new ClonkContentAssistant();
//		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
//		assistant.setContentAssistProcessor(new CodeBodyCompletionProcessor(getEditor(),assistant), ClonkPartitionScanner.C4S_CODEBODY);
		C4ScriptCompletionProcessor processor = new C4ScriptCompletionProcessor(editor(),assistant);
		for (String s : ClonkPartitionScanner.C4S_PARTITIONS)
			assistant.setContentAssistProcessor(processor, s);
		assistant.install(sourceViewer);
		
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);

		assistant.setRepeatedInvocationMode(true);
		// key sequence is set in constructor of ClonkCompletionProcessor
		
		assistant.setStatusLineVisible(true);
		assistant.setStatusMessage(Messages.C4ScriptSourceViewerConfiguration_StandardProposals);
		
		assistant.enablePrefixCompletion(false);
		assistant.enableAutoInsert(true);
		assistant.enableAutoActivation(true);
		
		assistant.enableColoredLabels(true);
		
		assistant.setInformationControlCreator(new IInformationControlCreator() {
			@Override
			public IInformationControl createInformationControl(Shell parent) {
//				BrowserInformationControl control = new BrowserInformationControl(parent, "Arial", "Press 'Tab' from proposal table or click for focus");
				DefaultInformationControl def = new DefaultInformationControl(parent,Messages.C4ScriptSourceViewerConfiguration_PressTabOrClick);
				return def;
			}
		});
		
		
		return assistant;
	}
	
	@Override
	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) { // noch unn�tz
		IQuickAssistAssistant assistant = new QuickAssistAssistant();
		assistant.setQuickAssistProcessor(new C4ScriptQuickAssistProcessor());
		return assistant;
	}
	
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		
		DefaultDamagerRepairer dr =
			new DefaultDamagerRepairer(clonkScanner());
		reconciler.setDamager(dr, ClonkPartitionScanner.C4S_CODEBODY);
		reconciler.setRepairer(dr, ClonkPartitionScanner.C4S_CODEBODY);
		
		dr = new DefaultDamagerRepairer(clonkScanner());
		reconciler.setDamager(dr, ClonkPartitionScanner.C4S_STRING);
		reconciler.setRepairer(dr, ClonkPartitionScanner.C4S_STRING);
		
		dr = new DefaultDamagerRepairer(clonkScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		
		dr = new DefaultDamagerRepairer(getClonkCommentScanner());
		reconciler.setDamager(dr, ClonkPartitionScanner.C4S_COMMENT);
		reconciler.setRepairer(dr, ClonkPartitionScanner.C4S_COMMENT);
		
		dr = new DefaultDamagerRepairer(getClonkCommentScanner());
		reconciler.setDamager(dr, ClonkPartitionScanner.C4S_MULTI_LINE_COMMENT);
		reconciler.setRepairer(dr, ClonkPartitionScanner.C4S_MULTI_LINE_COMMENT);
		
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
		return new IHyperlinkDetector[] {
			new C4ScriptHyperlinkDetector(),
			new URLHyperlinkDetector()
		};
	}
	
	private final C4ScriptAutoEditStrategy autoEditStrategy = new C4ScriptAutoEditStrategy(this);
	
	public C4ScriptAutoEditStrategy autoEditStrategy() {
		return autoEditStrategy;
	}

	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
		return new IAutoEditStrategy[] {autoEditStrategy};
	}
	
	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
	    if (hover == null)
	    	hover = new C4ScriptTextHover(this);
	    return hover;
	}
	
	@Override
	public void refreshSyntaxColoring() {
		clonkScanner().commitRules(getColorManager(), editor().scriptBeingEdited().engine());
	}

}