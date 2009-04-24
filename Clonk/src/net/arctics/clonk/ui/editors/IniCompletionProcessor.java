package net.arctics.clonk.ui.editors;

import java.util.Collection;
import java.util.LinkedList;

import net.arctics.clonk.ui.editors.c4script.WordScanner;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.texteditor.ITextEditor;

public class IniCompletionProcessor implements IContentAssistProcessor, ICompletionListener {

	private ITextEditor editor;
	private ContentAssistant assistant;
	
	public IniCompletionProcessor(ITextEditor editor, ContentAssistant assistant) {
		this.editor = editor;
		this.assistant = assistant;
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int offset) {
		Collection<ICompletionProposal> proposals = new LinkedList<ICompletionProposal>();
		
		int wordOffset = offset - 1;
		WordScanner scanner = new WordScanner();
		IDocument doc = viewer.getDocument();
		String prefix = null;
		try {
			while (scanner.isWordPart(doc.getChar(wordOffset))) {
				wordOffset--;
			}
			wordOffset++;
			if (wordOffset < offset) {
				prefix = doc.get(wordOffset, offset - wordOffset);
				
				offset = wordOffset;
			}
			if (prefix != null)
				prefix = prefix.toLowerCase();
		} catch (BadLocationException e) {
			prefix = null;
		}
		
		System.out.println(prefix);
		
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {
		// TODO Auto-generated method stub
		return null;
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	public char[] getContextInformationAutoActivationCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	public void assistSessionEnded(ContentAssistEvent event) {
		// TODO Auto-generated method stub
		
	}

	public void assistSessionStarted(ContentAssistEvent event) {
		// TODO Auto-generated method stub
		
	}

	public void selectionChanged(ICompletionProposal proposal,
			boolean smartToggle) {
		// TODO Auto-generated method stub
		
	}

}
