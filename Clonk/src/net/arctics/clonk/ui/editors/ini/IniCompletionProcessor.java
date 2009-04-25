package net.arctics.clonk.ui.editors.ini;

import java.util.Collection;
import java.util.LinkedList;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.parser.inireader.IniData.IniSectionData;
import net.arctics.clonk.ui.editors.ClonkCompletionProcessor;
import net.arctics.clonk.ui.editors.c4script.WordScanner;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.texteditor.ITextEditor;

public class IniCompletionProcessor extends ClonkCompletionProcessor implements ICompletionListener {
	
	public IniCompletionProcessor(ITextEditor editor, ContentAssistant assistant) {
		super(editor);
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
		
		if (prefix == null)
			prefix = "";
		else
			prefix = prefix.toLowerCase();
		
		try {
			IniUnit unit = Utilities.getIniUnitClass(Utilities.getEditingFile(editor)).getConstructor(String.class).newInstance(viewer.getDocument().get());
			unit.parse();
			IniSection currentSec = null;
			for (IniSection sec : unit.getSections()) {
				if (sec.getLocation().getStart() > offset)
					break;
				currentSec = sec;
			}
			if (currentSec != null) {
				IniSectionData d = currentSec.getSectionData();
				if (d != null) {
					for (IniDataEntry entry : d.getEntries().values()) {
						if (!entry.getEntryName().toLowerCase().contains(prefix))
							continue;
						proposals.add(new CompletionProposal(entry.getEntryName(), wordOffset, prefix.length(), entry.getEntryName().length()));
					}
				}
			}
			proposalsForIndexedObjects(ClonkCore.getDefault().EXTERN_INDEX, offset, wordOffset, prefix, proposals);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {
		return null;
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	public String getErrorMessage() {
		return null;
	}

	public void assistSessionEnded(ContentAssistEvent event) {
	}

	public void assistSessionStarted(ContentAssistEvent event) {
	}

	public void selectionChanged(ICompletionProposal proposal,
			boolean smartToggle) {
	}

}
