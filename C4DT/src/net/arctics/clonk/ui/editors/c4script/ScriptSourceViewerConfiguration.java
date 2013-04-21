package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.ui.editors.CStylePartitionScanner;
import net.arctics.clonk.ui.editors.ClonkHyperlink;
import net.arctics.clonk.ui.editors.ClonkRuleBasedScanner.ScannerPerEngine;
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
import org.eclipse.jface.text.contentassist.ContentAssistant;
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

public class ScriptSourceViewerConfiguration extends ClonkSourceViewerConfiguration<C4ScriptEditor> {

	public final class C4ScriptContentAssistant extends ContentAssistant {
		private ScriptCompletionProcessor processor;
		public C4ScriptContentAssistant(ISourceViewer sourceViewer) {
			processor = new ScriptCompletionProcessor(editor(), this);
			for (final String s : CStylePartitionScanner.PARTITIONS)
				setContentAssistProcessor(processor, s);
			install(sourceViewer);
			setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
			setRepeatedInvocationMode(true);
			setStatusLineVisible(true);
			setStatusMessage(Messages.C4ScriptSourceViewerConfiguration_StandardProposals);
			enablePrefixCompletion(false);
			enableAutoInsert(true);
			enableAutoActivation(true);
			setAutoActivationDelay(0);
			enableColoredLabels(true);
			setInformationControlCreator(new IInformationControlCreator() {
				@Override
				public IInformationControl createInformationControl(Shell parent) {
					final DefaultInformationControl def = new DefaultInformationControl(parent,Messages.C4ScriptSourceViewerConfiguration_PressTabOrClick);
					return def;
				}
			});
		}
		// make these public
		@Override
		public void hide() { super.hide(); }
		@Override
		public boolean isProposalPopupActive() { return super.isProposalPopupActive(); }
		public ScriptCompletionProcessor processor() { return processor; }
	}

	private class C4ScriptHyperlinkDetector implements IHyperlinkDetector {
		@Override
		public IHyperlink[] detectHyperlinks(ITextViewer viewer, IRegion region, boolean canShowMultipleHyperlinks) {
			try {
				final EntityLocator locator = new EntityLocator(editor(), viewer.getDocument(),region);
				if (locator.entity() != null)
					return new IHyperlink[] {
						new ClonkHyperlink(locator.expressionRegion(), locator.entity())
					};
				else if (locator.potentialEntities() != null)
					return new IHyperlink[] {
						new ClonkHyperlink(locator.expressionRegion(), locator.potentialEntities())
					};
				return null;
			} catch (final Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	private static ScannerPerEngine<ScriptCodeScanner> SCANNERS = new ScannerPerEngine<ScriptCodeScanner>(ScriptCodeScanner.class);

	private ITextDoubleClickStrategy doubleClickStrategy;
	private ContentAssistant contentAssistant;
	private final ScriptAutoEditStrategy autoEditStrategy = new ScriptAutoEditStrategy(this);

	public ScriptSourceViewerConfiguration(IPreferenceStore store, ColorManager colorManager, C4ScriptEditor textEditor) {
		super(store, colorManager, textEditor);
	}

	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return CStylePartitionScanner.PARTITIONS;
	}

	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new ScriptDoubleClickStrategy(this);
		return doubleClickStrategy;
	}

	@Override
	public ContentAssistant getContentAssistant(final ISourceViewer sourceViewer) {
		if (contentAssistant == null)
			contentAssistant = new C4ScriptContentAssistant(sourceViewer);
		return contentAssistant;
	}

	@Override
	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		final IQuickAssistAssistant assistant = new QuickAssistAssistant();
		assistant.setQuickAssistProcessor(new ScriptQuickAssistProcessor());
		return assistant;
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		final PresentationReconciler reconciler = new PresentationReconciler();
		final ScriptCommentScanner commentScanner = new ScriptCommentScanner(getColorManager(), "COMMENT"); //$NON-NLS-1$
		final ScriptCodeScanner scanner = SCANNERS.get(this.editor().script().engine());

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, CStylePartitionScanner.CODEBODY);
		reconciler.setRepairer(dr, CStylePartitionScanner.CODEBODY);

		dr = new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, CStylePartitionScanner.STRING);
		reconciler.setRepairer(dr, CStylePartitionScanner.STRING);

		dr = new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(new ScriptCommentScanner(getColorManager(), "JAVADOCCOMMENT"));
		reconciler.setDamager(dr, CStylePartitionScanner.JAVADOC_COMMENT);
		reconciler.setRepairer(dr, CStylePartitionScanner.JAVADOC_COMMENT);

		dr = new DefaultDamagerRepairer(commentScanner);
		reconciler.setDamager(dr, CStylePartitionScanner.COMMENT);
		reconciler.setRepairer(dr, CStylePartitionScanner.COMMENT);

		dr = new DefaultDamagerRepairer(commentScanner);
		reconciler.setDamager(dr, CStylePartitionScanner.MULTI_LINE_COMMENT);
		reconciler.setRepairer(dr, CStylePartitionScanner.MULTI_LINE_COMMENT);

		return reconciler;
	}

	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		return new IHyperlinkDetector[] {
			new C4ScriptHyperlinkDetector(),
			new URLHyperlinkDetector()
		};
	}

	public ScriptAutoEditStrategy autoEditStrategy() { return autoEditStrategy; }

	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
		return new IAutoEditStrategy[] {autoEditStrategy};
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
	    if (hover == null)
	    	hover = new ScriptTextHover(this);
	    return hover;
	}

}