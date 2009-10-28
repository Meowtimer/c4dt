package net.arctics.clonk.ui.editors.ini;

import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.c4script.C4Variable.C4VariableScope;
import net.arctics.clonk.parser.inireader.Boolean;
import net.arctics.clonk.parser.inireader.CategoriesArray;
import net.arctics.clonk.parser.inireader.DefinitionPack;
import net.arctics.clonk.parser.inireader.Function;
import net.arctics.clonk.parser.inireader.IDArray;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.parser.inireader.SignedInteger;
import net.arctics.clonk.parser.inireader.UnsignedInteger;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.parser.inireader.IniData.IniSectionData;
import net.arctics.clonk.resource.ExternalLib;
import net.arctics.clonk.ui.editors.ClonkCompletionProcessor;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

/**
 * Completion processor for ini files. Proposes entries and values for those entries based on their type (functions from the related script for callbacks for example)
 * @author madeen
 *
 */
public class IniCompletionProcessor extends ClonkCompletionProcessor<IniTextEditor> implements ICompletionListener {

	private IniSection section;

	public IniCompletionProcessor(IniTextEditor editor, ContentAssistant assistant) {
		super(editor);
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		Collection<ICompletionProposal> proposals = new LinkedList<ICompletionProposal>();

		IDocument doc = viewer.getDocument();
		String line;
		int lineStart;
		try {
			IRegion lineRegion = doc.getLineInformationOfOffset(offset);
			line = doc.get(lineRegion.getOffset(), lineRegion.getLength());
			lineStart = lineRegion.getOffset();
		} catch (BadLocationException e) {
			line = ""; //$NON-NLS-1$
			lineStart = offset;
		}

		Matcher m;
		String prefix = ""; //$NON-NLS-1$
		String entryName = ""; //$NON-NLS-1$
		boolean assignment = false;
		int wordOffset = offset;
		if ((m = IniSourceViewerConfiguration.ASSIGN_PATTERN.matcher(line)).matches()) {
			entryName = m.group(1);
			prefix = m.group(2);
			assignment = true;
			wordOffset = lineStart + m.start(2); 
		}
		else if ((m = IniSourceViewerConfiguration.NO_ASSIGN_PATTERN.matcher(line)).matches()) {
			prefix = m.group(1);
			wordOffset = lineStart + m.start(1);
		}
		prefix = prefix.toLowerCase();

		section = getEditor().getIniUnit().sectionAtOffset(lineStart, line.length());

		if (!assignment) {
			if (section != null) {
				IniSectionData d = section.getSectionData();
				if (d != null) {
					proposalsForSection(proposals, prefix, wordOffset, d);
				}
				// also propose new sections
				IniUnit unit = getEditor().getIniUnit();
				proposalsForIniUnit(proposals, prefix, wordOffset, unit);
			}
		}
		else if (assignment && section != null) {
			IniDataEntry entryDef = section.getSectionData().getEntry(entryName);
			Class<?> entryClass = entryDef.getEntryClass();
			if (entryClass == C4ID.class) {
				proposalsForIndex(offset, proposals, prefix, wordOffset);
			}
			else if (entryClass == String.class) {
				proposalsForStringEntry(proposals, prefix, wordOffset);
			}
			else if (entryClass == SignedInteger.class || entryClass == UnsignedInteger.class) {
				proposalsForIntegerEntry(proposals, prefix, wordOffset);
			}
			else if (entryClass == Function.class) {
				proposalsForFunctionEntry(proposals, prefix, wordOffset);
			}
			else if (entryClass == IDArray.class) {
				int lastDelim = prefix.lastIndexOf(';');
				prefix = prefix.substring(lastDelim+1);
				wordOffset += lastDelim+1;
				proposalsForIndex(offset, proposals, prefix, wordOffset);
			}
			else if (entryClass == Boolean.class) {
				proposalsForBooleanEntry(proposals, prefix, wordOffset);
			}
			else if (entryClass == DefinitionPack.class) {
				proposalsForDefinitionPackEntry(proposals, prefix, wordOffset);
			}
			else if (entryClass == CategoriesArray.class) {
				int lastDelim = prefix.lastIndexOf('|');
				prefix = prefix.substring(lastDelim+1);
				wordOffset += lastDelim+1;
				proposalsForCategoriesArray(proposals, prefix, wordOffset, entryDef);
			}
		}

		return sortProposals(proposals);
	}

	private void proposalsForCategoriesArray(Collection<ICompletionProposal> proposals, String prefix, int wordOffset, IniDataEntry entryDef) {
		if (entryDef.getFlags() != null) {
			for (String flag : entryDef.getFlags()) {
				if (flag.toLowerCase().contains(prefix))
					proposals.add(new CompletionProposal(flag, wordOffset, prefix.length(), flag.length()));
			}
		}
		else {
			for (C4Variable v : ClonkCore.getDefault().getActiveEngine().variables()) {
				if (v.getScope() == C4VariableScope.VAR_CONST) {
					proposalForVar(v, prefix, wordOffset, proposals);
				}
			}
		}
	}

	private void proposalsForIndex(int offset, Collection<ICompletionProposal> proposals, String prefix, int wordOffset) {
		proposalsForIndexedObjects(ClonkCore.getDefault().getExternIndex(), offset, wordOffset, prefix, proposals);
		ClonkIndex index = Utilities.getIndex(getEditor().getIniUnit().getIniFile());
		if (index != null)
			proposalsForIndexedObjects(index, offset, wordOffset, prefix, proposals);
	}

	private void proposalsForDefinitionPackEntry(Collection<ICompletionProposal> proposals, String prefix, int wordOffset) {
		for (ExternalLib lib : ClonkCore.getDefault().getExternIndex().getLibs()) {
			if (!lib.getNodeName().toLowerCase().contains(prefix))
				continue;
			if (!lib.getNodeName().endsWith(".c4d")) //$NON-NLS-1$
				continue;
			proposals.add(new CompletionProposal(lib.getNodeName(), wordOffset, prefix.length(), lib.getNodeName().length()));
		}
	}

	private void proposalsForIniUnit(Collection<ICompletionProposal> proposals, String prefix, int wordOffset, IniUnit unit) {
		for (IniSectionData sec : unit.getConfiguration().getSections().values()) {
			if (sec.getSectionName().toLowerCase().contains(prefix)) {
				String secString = "["+sec.getSectionName()+"]"; //$NON-NLS-1$ //$NON-NLS-2$
				proposals.add(new CompletionProposal(secString, wordOffset, prefix.length(), secString.length(), null, null, null, "ugh")); //$NON-NLS-1$
			}
		}
	}

	private void proposalsForSection(Collection<ICompletionProposal> proposals, String prefix, int wordOffset, IniSectionData sectionData) {
		for (IniDataEntry entry : sectionData.getEntries().values()) {
			if (!entry.getEntryName().toLowerCase().contains(prefix))
				continue;
			proposals.add(new CompletionProposal(entry.getEntryName(), wordOffset, prefix.length(), entry.getEntryName().length(), null, entry.getEntryName(), null, entry.getDescription()));
		}
	}

	private void proposalsForFunctionEntry(Collection<ICompletionProposal> proposals, String prefix, int wordOffset) {
		C4ObjectIntern obj = C4ObjectIntern.objectCorrespondingTo(Utilities.getEditingFile(editor).getParent());
		if (obj != null) {
			for (C4ScriptBase script : obj.conglomerate()) {
				for (C4Function f : script.functions()) {
					proposalForFunc(f, prefix, wordOffset, proposals, script.getName(), false);
				}
			}
		}
	}

	private void proposalsForBooleanEntry(Collection<ICompletionProposal> proposals, String prefix, int wordOffset) {
		int[] choices = new int[] {0, 1};
		for (int i : choices) {
			proposals.add(new CompletionProposal(String.valueOf(i), wordOffset, prefix.length(), String.valueOf(i).length()));
		}
	}

	private void proposalsForIntegerEntry(Collection<ICompletionProposal> proposals, String prefix, int wordOffset) {
		// KillaRitter ist langweilig -.-
		//		int[] awesomeNumbers = new int[] {42, 1337, 1984};
		//		for (int i : awesomeNumbers) {
		//			proposals.add(new CompletionProposal(String.valueOf(i), wordOffset, prefix.length(), String.valueOf(i).length()));
		//		}
	}

	private void proposalsForStringEntry(Collection<ICompletionProposal> proposals, String prefix, int wordOffset) {
		// KillaRitter ist langweilig -.-
		//		String[] awesomeProposals = new String[] {"Super Ultra Flint 5001", "Klonfabrik"};
		//		for (String awesomeProposal : awesomeProposals)
		//			proposals.add(new CompletionProposal(awesomeProposal, wordOffset, prefix.length(), awesomeProposal.length()));
	}

	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] {'='};
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	public void assistSessionEnded(ContentAssistEvent event) {
		getEditor().unlockUnit();
	}

	public void assistSessionStarted(ContentAssistEvent event) {
		try {
			getEditor().forgetUnitParsed();
			getEditor().lockUnit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
	}

}
