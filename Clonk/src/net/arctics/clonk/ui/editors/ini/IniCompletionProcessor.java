package net.arctics.clonk.ui.editors.ini;

import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.C4Function;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.C4ObjectIntern;
import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.parser.inireader.Boolean;
import net.arctics.clonk.parser.inireader.CategoriesArray;
import net.arctics.clonk.parser.inireader.Function;
import net.arctics.clonk.parser.inireader.IDArray;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.parser.inireader.KeyValueArrayEntry;
import net.arctics.clonk.parser.inireader.SignedInteger;
import net.arctics.clonk.parser.inireader.UnsignedInteger;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.parser.inireader.IniData.IniSectionData;
import net.arctics.clonk.ui.editors.ClonkCompletionProcessor;
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
	
	private static Pattern noAssignPattern = Pattern.compile("([A-Za-z_0-9]*)");
	private static Pattern assignPattern = Pattern.compile("([A-Za-z_0-9]*)=(.*)");
	
	private IniUnit unit;
	private IniSection section;
	
	public IniCompletionProcessor(ITextEditor editor, ContentAssistant assistant) {
		super(editor);
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int offset) {
		Collection<ICompletionProposal> proposals = new LinkedList<ICompletionProposal>();
		
		IDocument doc = viewer.getDocument();
		String line;
		int lineStart;
		try {
			for (lineStart = offset-1; lineStart >= 0; lineStart--) {
				if (BufferedScanner.isLineDelimiterChar(doc.getChar(lineStart))) {
					lineStart++;
					break;
				}
			}
			line = doc.get(lineStart, offset-lineStart); 
		} catch (BadLocationException e) {
			line = "";
			lineStart = offset;
		}
		
		Matcher m;
		String prefix = "";
		String entryName = "";
		boolean assignment = false;
		int wordOffset = offset;
		if ((m = assignPattern.matcher(line)).matches()) {
			entryName = m.group(1);
			prefix = m.group(2);
			assignment = true;
			wordOffset = lineStart + m.start(2); 
		}
		else if ((m = noAssignPattern.matcher(line)).matches()) {
			prefix = m.group(1);
			wordOffset = lineStart + m.start(1);
		}
		prefix = prefix.toLowerCase();
		
		section = null;
		for (IniSection sec : unit.getSections()) {
			int start = sec.getLocation().getStart();
			if (start > lineStart)
				start += line.length();
			if (start > offset)
				break;
			section = sec;
		}

		if (!assignment) {
			if (section != null) {
				IniSectionData d = section.getSectionData();
				if (d != null) {
					for (IniDataEntry entry : d.getEntries().values()) {
						if (!entry.getEntryName().toLowerCase().contains(prefix))
							continue;
						proposals.add(new CompletionProposal(entry.getEntryName(), wordOffset, prefix.length(), entry.getEntryName().length()));
					}
				}
			}
		}
		else if (assignment && section != null) {
			IniDataEntry entryDef = section.getSectionData().getEntries().get(entryName);
			if (entryDef.getEntryClass() == C4ID.class) {
				proposalsForIndexedObjects(ClonkCore.getDefault().EXTERN_INDEX, offset, wordOffset, prefix, proposals);
			}
			else if (entryDef.getEntryClass() == String.class) {
				proposalsForStringEntry(proposals, prefix, wordOffset);
			}
			else if (entryDef.getEntryClass() == SignedInteger.class || entryDef.getEntryClass() == UnsignedInteger.class) {
				proposalsForIntegerEntry(proposals, prefix, wordOffset);
			}
			else if (entryDef.getEntryClass() == Function.class) {
				proposalsForFunctionEntry(proposals, prefix, wordOffset);
			}
			else if (entryDef.getEntryClass() == IDArray.class) {
				int lastDelim = prefix.lastIndexOf(';');
				prefix = prefix.substring(lastDelim+1);
				wordOffset += lastDelim+1;
				proposalsForIndexedObjects(ClonkCore.getDefault().EXTERN_INDEX, offset, wordOffset, prefix, proposals);
			}
			else if (entryDef.getEntryClass() == Boolean.class) {
				proposalsForBooleanEntry(proposals, prefix, wordOffset);
			}
		}
		
		return sortProposals(proposals.toArray(new ICompletionProposal[proposals.size()]));
	}

	private void proposalsForFunctionEntry(
			Collection<ICompletionProposal> proposals, String prefix,
			int wordOffset) {
		C4ObjectIntern obj = C4ObjectIntern.objectCorrespondingTo(Utilities.getEditingFile(editor).getParent());
		if (obj != null) {
			for (C4ScriptBase script : obj.conglomerate()) {
				for (C4Function f : script.functions()) {
					proposalForFunc(f, prefix, wordOffset, proposals, script.getName(), false);
				}
			}
		}
	}

	private void proposalsForBooleanEntry(
			Collection<ICompletionProposal> proposals, String prefix,
			int wordOffset) {
		int[] choices = new int[] {0, 1};
		for (int i : choices) {
			proposals.add(new CompletionProposal(String.valueOf(i), wordOffset, prefix.length(), String.valueOf(i).length()));
		}
	}
	
	private void proposalsForIntegerEntry(
			Collection<ICompletionProposal> proposals, String prefix,
			int wordOffset) {
		int[] awesomeNumbers = new int[] {42, 1337, 1984};
		for (int i : awesomeNumbers) {
			proposals.add(new CompletionProposal(String.valueOf(i), wordOffset, prefix.length(), String.valueOf(i).length()));
		}
	}

	private void proposalsForStringEntry(
			Collection<ICompletionProposal> proposals, String prefix,
			int wordOffset) {
		String[] awesomeProposals = new String[] {"Super Ultra Flint 5001", "Klonfabrik"};
		for (String awesomeProposal : awesomeProposals)
			proposals.add(new CompletionProposal(awesomeProposal, wordOffset, prefix.length(), awesomeProposal.length()));
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
		unit = null;
	}

	public void assistSessionStarted(ContentAssistEvent event) {
		try {
			unit = Utilities.getIniUnitClass(Utilities.getEditingFile(editor)).getConstructor(String.class).newInstance(editor.getDocumentProvider().getDocument(editor.getEditorInput()).get());
			unit.parse();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void selectionChanged(ICompletionProposal proposal,
			boolean smartToggle) {
	}

}
