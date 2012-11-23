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
import net.arctics.clonk.parser.inireader.CategoriesValue;
import net.arctics.clonk.parser.inireader.DefinitionPack;
import net.arctics.clonk.parser.inireader.FunctionEntry;
import net.arctics.clonk.parser.inireader.IDArray;
import net.arctics.clonk.parser.inireader.IconSpec;
import net.arctics.clonk.parser.inireader.IniData.IniDataBase;
import net.arctics.clonk.parser.inireader.IniData.IniEntryDefinition;
import net.arctics.clonk.parser.inireader.IniData.IniSectionDefinition;
import net.arctics.clonk.parser.inireader.IniSection;
import net.arctics.clonk.parser.inireader.IniUnit;
import net.arctics.clonk.parser.inireader.SignedInteger;
import net.arctics.clonk.parser.inireader.UnsignedInteger;
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

	@Override
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

		editor().ensureIniUnitUpToDate();
		section = editor().unit().sectionAtOffset(offset);

		if (!assignment) {
			if (section != null) {
				IniSectionDefinition d = section.sectionData();
				if (d != null)
					proposalsForSection(proposals, prefix, wordOffset, d);
				if (section.parentSection() != null && section.parentSection().sectionData() != null)
					// also propose new sections
					proposalsForIniDataEntries(proposals, prefix, wordOffset, section.parentSection().sectionData().entries().values());
				else if (section.parentDeclaration() instanceof IniUnit)
					proposalsForIniDataEntries(proposals, prefix, wordOffset, ((IniUnit)section.parentDeclaration()).configuration().sections().values());
				int indentation = editor().unit().parser().indentationAt(offset);
				if (indentation == section.indentation()+1)
					proposalsForIniDataEntries(proposals, prefix, wordOffset, section.sectionData().entries().values());
			}
		}
		else if (assignment && section != null) {
			IniDataBase itemData = section.sectionData().entryForKey(entryName);
			if (itemData instanceof IniEntryDefinition) {
				IniEntryDefinition entryDef = (IniEntryDefinition) itemData;
				Class<?> entryClass = entryDef.entryClass();
				if (entryClass == ID.class || entryClass == IconSpec.class)
					proposalsForIndex(offset, proposals, prefix, wordOffset);
				else if (entryClass == String.class)
					proposalsForStringEntry(proposals, prefix, wordOffset);
				else if (entryClass == SignedInteger.class || entryClass == UnsignedInteger.class)
					proposalsForIntegerEntry(proposals, prefix, wordOffset);
				else if (entryClass == FunctionEntry.class)
					proposalsForFunctionEntry(proposals, prefix, wordOffset);
				else if (entryClass == IDArray.class) {
					int lastDelim = prefix.lastIndexOf(';');
					prefix = prefix.substring(lastDelim+1);
					wordOffset += lastDelim+1;
					proposalsForIndex(offset, proposals, prefix, wordOffset);
				}
				else if (entryClass == Boolean.class)
					proposalsForBooleanEntry(proposals, prefix, wordOffset);
				else if (entryClass == DefinitionPack.class)
					proposalsForDefinitionPackEntry(proposals, prefix, wordOffset);
				else if (entryClass == CategoriesValue.class) {
					int lastDelim = prefix.lastIndexOf('|');
					prefix = prefix.substring(lastDelim+1);
					wordOffset += lastDelim+1;
					proposalsForCategoriesValue(proposals, prefix, wordOffset, entryDef);
				}
			}
		}

		return sortProposals(proposals);
	}

	private void proposalsForCategoriesValue(Collection<ICompletionProposal> proposals, String prefix, int wordOffset, IniEntryDefinition entryDef) {
		if (prefix != null)
			for (Variable v : editor().unit().engine().variablesWithPrefix(entryDef.constantsPrefix()))
				if (v.scope() == Scope.CONST)
					proposalForVar(v, prefix, wordOffset, proposals);
	}

	private void proposalsForIndex(int offset, Collection<ICompletionProposal> proposals, String prefix, int wordOffset) {
		Index index = ProjectIndex.fromResource(editor().unit().iniFile());
		if (index != null)
			for (Index i : index.relevantIndexes())
				proposalsForIndexedDefinitions(i, offset, wordOffset, prefix, proposals);
	}

	private void proposalsForDefinitionPackEntry(Collection<ICompletionProposal> proposals, String prefix, int wordOffset) {
		ClonkProjectNature nature = ClonkProjectNature.get(this.editor.topLevelDeclaration().resource().getProject());
		List<Index> indexes = nature.index().relevantIndexes();
		for (Index index : indexes)
			if (index instanceof ProjectIndex)
				try {
					for (IResource res : ((ProjectIndex)index).project().members())
						if (res instanceof IContainer && nature.index().engine().groupTypeForFileName(res.getName()) == GroupType.DefinitionGroup)
							if (res.getName().toLowerCase().contains(prefix))
								proposals.add(new CompletionProposal(res.getName(), wordOffset, prefix.length(), res.getName().length()));
				} catch (CoreException e) {
					e.printStackTrace();
				}
	}

	private void proposalsForIniDataEntries(Collection<ICompletionProposal> proposals, String prefix, int wordOffset, Iterable<? extends IniDataBase> sectionData) {
		for (IniDataBase sec : sectionData)
			if (sec instanceof IniSectionDefinition && ((IniSectionDefinition) sec).sectionName().toLowerCase().contains(prefix)) {
				String secString = "["+((IniSectionDefinition) sec).sectionName()+"]"; //$NON-NLS-1$ //$NON-NLS-2$
				proposals.add(new CompletionProposal(secString, wordOffset, prefix.length(), secString.length(), null, null, null, "ugh")); //$NON-NLS-1$
			}
	}

	private void proposalsForSection(Collection<ICompletionProposal> proposals, String prefix, int wordOffset, IniSectionDefinition sectionData) {
		for (IniDataBase entry : sectionData.entries().values())
			if (entry instanceof IniEntryDefinition) {
				IniEntryDefinition e = (IniEntryDefinition) entry;
				if (!e.entryName().toLowerCase().contains(prefix))
					continue;
				proposals.add(new CompletionProposal(e.entryName(), wordOffset, prefix.length(), e.entryName().length(), null, e.entryName(), null, e.description()));
			}
			else if (entry instanceof IniSectionDefinition) {
				// FIXME
			}
	}

	private void proposalsForFunctionEntry(Collection<ICompletionProposal> proposals, String prefix, int wordOffset) {
		Definition obj = Definition.definitionCorrespondingToFolder(Utilities.fileEditedBy(editor).getParent());
		if (obj != null)
			for (IHasIncludes include : obj.conglomerate()) {
				Script script = Utilities.as(include, Script.class);
				if (script == null)
					continue;
				for (Function f : script.functions())
					proposalForFunc(f, prefix, wordOffset, proposals, script.name(), false);
			}
	}

	private void proposalsForBooleanEntry(Collection<ICompletionProposal> proposals, String prefix, int wordOffset) {
		int[] choices = new int[] {0, 1};
		for (int i : choices)
			proposals.add(new CompletionProposal(String.valueOf(i), wordOffset, prefix.length(), String.valueOf(i).length()));
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

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] {'='};
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	@Override
	public void assistSessionEnded(ContentAssistEvent event) {
		editor().unlockUnit();
	}

	@Override
	public void assistSessionStarted(ContentAssistEvent event) {
		try {
			editor().forgetUnitParsed();
			editor().lockUnit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
	}

}
