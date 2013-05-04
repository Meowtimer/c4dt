package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.ui.editors.CStylePartitionScanner;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Shell;

public final class ScriptContentAssistant extends ContentAssistant {
	private ScriptCompletionProcessor processor;
	public ScriptContentAssistant(ScriptSourceViewerConfiguration configuration, ISourceViewer sourceViewer) {
		processor = new ScriptCompletionProcessor(configuration.editor(), this);
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