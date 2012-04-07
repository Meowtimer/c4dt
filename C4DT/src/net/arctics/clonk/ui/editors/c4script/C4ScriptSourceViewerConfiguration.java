package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.ui.editors.ClonkContentAssistant;
import net.arctics.clonk.ui.editors.ClonkHyperlink;
import net.arctics.clonk.ui.editors.ClonkPartitionScanner;
import net.arctics.clonk.ui.editors.ClonkSourceViewerConfiguration;
import net.arctics.clonk.ui.editors.ColorManager;
import net.arctics.clonk.ui.editors.ScriptCommentScanner;
import net.arctics.clonk.ui.editors.ClonkRuleBasedScanner.ScannerPerEngine;

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
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
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
	
	private static ScannerPerEngine<C4ScriptCodeScanner> SCANNERS = new ScannerPerEngine<C4ScriptCodeScanner>(C4ScriptCodeScanner.class);
	
	private ITextDoubleClickStrategy doubleClickStrategy;

	public C4ScriptSourceViewerConfiguration(IPreferenceStore store, ColorManager colorManager, C4ScriptEditor textEditor) {
		super(store, colorManager, textEditor);
	}
	
	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return ClonkPartitionScanner.PARTITIONS;
	}
	
	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new C4ScriptDoubleClickStrategy(this);
		return doubleClickStrategy;
	}

	private ClonkContentAssistant assistant;
	
	@Override
	public ClonkContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		if (assistant != null)
			return assistant;
		
		assistant = new ClonkContentAssistant();
		C4ScriptCompletionProcessor processor = new C4ScriptCompletionProcessor(editor(),assistant);
		for (String s : ClonkPartitionScanner.PARTITIONS)
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
		
		ScriptCommentScanner commentScanner = new ScriptCommentScanner(getColorManager(), "COMMENT"); //$NON-NLS-1$
		
		C4ScriptCodeScanner scanner = SCANNERS.get(this.editor().script().engine());
		
		DefaultDamagerRepairer dr =
			new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, ClonkPartitionScanner.CODEBODY);
		reconciler.setRepairer(dr, ClonkPartitionScanner.CODEBODY);
		
		dr = new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, ClonkPartitionScanner.STRING);
		reconciler.setRepairer(dr, ClonkPartitionScanner.STRING);
		
		dr = new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		
		dr = new DefaultDamagerRepairer(new ScriptCommentScanner(getColorManager(), "JAVADOCCOMMENT"));
		reconciler.setDamager(dr, ClonkPartitionScanner.JAVADOC_COMMENT);
		reconciler.setRepairer(dr, ClonkPartitionScanner.JAVADOC_COMMENT);
		
		dr = new DefaultDamagerRepairer(commentScanner);
		reconciler.setDamager(dr, ClonkPartitionScanner.COMMENT);
		reconciler.setRepairer(dr, ClonkPartitionScanner.COMMENT);
		
		dr = new DefaultDamagerRepairer(commentScanner);
		reconciler.setDamager(dr, ClonkPartitionScanner.MULTI_LINE_COMMENT);
		reconciler.setRepairer(dr, ClonkPartitionScanner.MULTI_LINE_COMMENT);
		
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

}