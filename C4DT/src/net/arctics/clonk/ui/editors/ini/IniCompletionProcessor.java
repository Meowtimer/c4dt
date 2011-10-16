package net.arctics.clonk.ui.editors.ini;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.ProjectIndex;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.IHasIncludes;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.parser.inireader.Boolean;
import net.arctics.clonk.parser.inireader.CategoriesArray;
import net.arctics.clonk.parser.inireader.DefinitionPack;
import net.arctics.clonk.parser.inireader.FuncRefEntry;
import net.arctics.clonk.parser.inireader.IDArray;
import net.arctics.clonk.parser.inireader.IconSpec;
import net.arctics.clonk.parser.inireader.IniData.IniDataBase;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.parser.inireader.SignedInteger;
import net.arctics.clonk.parser.inireader.UnsignedInteger;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.parser.inireader.IniData.IniDataSection;
import net.arctics.clonk.resource.ClonkProjectNature;
import net.arctics.clonk.resource.c4group.C4Group.GroupType;
import net.arctics.clonk.ui.editors.ClonkCompletionProcessor;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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

		getEditor().ensureIniUnitUpToDate();
		section = getEditor().getIniUnit().sectionAtOffset(offset);

		if (!assignment) {
			if (section != null) {
				IniDataSection d = section.getSectionData();
				if (d != null) {
					proposalsForSection(proposals, prefix, wordOffset, d);
				}
				if (section.parentSection() != null && section.parentSection().getSectionData() != null) {
					// also propose new sections
					proposalsForIniDataEntries(proposals, prefix, wordOffset, section.parentSection().getSectionData().getEntries().values());
				} else if (section.getParentDeclaration() instanceof IniUnit) {
					proposalsForIniDataEntries(proposals, prefix, wordOffset, ((IniUnit)section.getParentDeclaration()).getConfiguration().getSections().values());
				}
				int indentation = getEditor().getIniUnit().getParser().getTabIndentation(offset);
				if (indentation == section.getIndentation()+1) {
					proposalsForIniDataEntries(proposals, prefix, wordOffset, section.getSectionData().getEntries().values());
				}
			}
		}
		else if (assignment && section != null) {
			IniDataBase itemData = section.getSectionData().getEntry(entryName);
			if (itemData instanceof IniDataEntry) {
				IniDataEntry entryDef = (IniDataEntry) itemData;
				Class<?> entryClass = entryDef.getEntryClass();
				if (entryClass == ID.class || entryClass == IconSpec.class) {
					proposalsForIndex(offset, proposals, prefix, wordOffset);
				}
				else if (entryClass == String.class) {
					proposalsForStringEntry(proposals, prefix, wordOffset);
				}
				else if (entryClass == SignedInteger.class || entryClass == UnsignedInteger.class) {
					proposalsForIntegerEntry(proposals, prefix, wordOffset);
				}
				else if (entryClass == FuncRefEntry.class) {
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
		}

		return sortProposals(proposals);
	}

	private void proposalsForCategoriesArray(Collection<ICompletionProposal> proposals, String prefix, int wordOffset, IniDataEntry entryDef) {
		if (prefix != null) {
			for (Variable v : getEditor().getIniUnit().getEngine().variablesWithPrefix(entryDef.getConstantsPrefix())) {
				if (v.getScope() == Scope.CONST) {
					proposalForVar(v, prefix, wordOffset, proposals);
				}
			}
		}
	}

	private void proposalsForIndex(int offset, Collection<ICompletionProposal> proposals, String prefix, int wordOffset) {
		Index index = Utilities.getIndex(getEditor().getIniUnit().getIniFile());
		if (index != null) {
			for (Index i : index.relevantIndexes()) {
				proposalsForIndexedDefinitions(i, offset, wordOffset, prefix, proposals);
			}
		}
	}

	private void proposalsForDefinitionPackEntry(Collection<ICompletionProposal> proposals, String prefix, int wordOffset) {
		ClonkProjectNature nature = ClonkProjectNature.get(this.editor.topLevelDeclaration().getResource().getProject());
		List<Index> indexes = nature.getIndex().relevantIndexes();
		for (Index index : indexes) {
			if (index instanceof ProjectIndex) {
				try {
					for (IResource res : ((ProjectIndex)index).getProject().members()) {
						if (res instanceof IContainer && nature.getIndex().getEngine().getGroupTypeForFileName(res.getName()) == GroupType.DefinitionGroup)
							if (res.getName().toLowerCase().contains(prefix))
								proposals.add(new CompletionProposal(res.getName(), wordOffset, prefix.length(), res.getName().length()));
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void proposalsForIniDataEntries(Collection<ICompletionProposal> proposals, String prefix, int wordOffset, Iterable<? extends IniDataBase> sectionData) {
		for (IniDataBase sec : sectionData) {
			if (sec instanceof IniDataSection && ((IniDataSection) sec).getSectionName().toLowerCase().contains(prefix)) {
				String secString = "["+((IniDataSection) sec).getSectionName()+"]"; //$NON-NLS-1$ //$NON-NLS-2$
				proposals.add(new CompletionProposal(secString, wordOffset, prefix.length(), secString.length(), null, null, null, "ugh")); //$NON-NLS-1$
			}
		}
	}

	private void proposalsForSection(Collection<ICompletionProposal> proposals, String prefix, int wordOffset, IniDataSection sectionData) {
		for (IniDataBase entry : sectionData.getEntries().values()) {
			if (entry instanceof IniDataEntry) {
				IniDataEntry e = (IniDataEntry) entry;
				if (!e.getEntryName().toLowerCase().contains(prefix))
					continue;
				proposals.add(new CompletionProposal(e.getEntryName(), wordOffset, prefix.length(), e.getEntryName().length(), null, e.getEntryName(), null, e.getDescription()));
			}
			else if (entry instanceof IniDataSection) {
				// FIXME
			}
		}
	}

	private void proposalsForFunctionEntry(Collection<ICompletionProposal> proposals, String prefix, int wordOffset) {
		Definition obj = Definition.definitionCorrespondingToFolder(Utilities.getFileBeingEditedBy(editor).getParent());
		if (obj != null) {
			for (IHasIncludes include : obj.conglomerate()) {
				Script script = Utilities.as(include, Script.class);
				if (script == null)
					continue;
				for (Function f : script.functions()) {
					proposalForFunc(f, prefix, wordOffset, proposals, script.name(), false);
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
