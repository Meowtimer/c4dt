package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.Utilities.as;
import net.arctics.clonk.ui.editors.c4script.C4ScriptCompletionProcessor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;

public class ClonkContentAssistant extends ContentAssistant {
	private ITextViewer viewer;
	@Override
	public void install(ITextViewer textViewer) {
		super.install(textViewer);
		this.viewer = textViewer;
	}
	@Override
	public void hide() {
		super.hide();
	}
	@Override
	public boolean isContextInfoPopupActive() {
		return super.isContextInfoPopupActive();
	}
	@Override
	public boolean isProposalPopupActive() {
		return super.isProposalPopupActive();
	}
	// pasta
	private IContentAssistProcessor getProcessor(ITextViewer viewer, int offset) {
		try {

			IDocument document= viewer.getDocument();
			String type= TextUtilities.getContentType(document, getDocumentPartitioning(), offset, true);
			return getContentAssistProcessor(type);
		} catch (BadLocationException x) {}
		return null;
	}
	@Override
	public String showPossibleCompletions() {
		C4ScriptCompletionProcessor processor = as(getProcessor(viewer, viewer.getSelectedRange().x), C4ScriptCompletionProcessor.class);
		if (processor != null) {
			
		}
		return super.showPossibleCompletions();
	}
}