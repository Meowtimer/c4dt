package net.arctics.clonk.ui.editors;

import static net.arctics.clonk.util.Utilities.as;

import java.util.Collection;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal.Category;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalSorter;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.ui.IFileEditorInput;

public abstract class ClonkCompletionProcessor<EditorType extends ClonkTextEditor> implements IContentAssistProcessor, ICompletionProposalSorter {

	protected EditorType editor;
	protected String prefix;
	
	public EditorType editor() { return editor; }
	public ClonkCompletionProcessor(EditorType editor, ContentAssistant assistant) {
		this.editor = editor;
		assistant.setSorter(this);
	}
	
	protected void proposalForDefinition(Definition def, String prefix, int offset, Collection<ICompletionProposal> proposals) {
		try {
			if (def == null || def.id() == null)
				return;

			if (prefix != null)
				if (!(
					stringMatchesPrefix(def.name(), prefix) ||
					stringMatchesPrefix(def.id().stringValue(), prefix) ||
					// also check if the user types in the folder name
					(def instanceof Definition && def.definitionFolder() != null &&
					 stringMatchesPrefix(def.definitionFolder().getName(), prefix))
				))
					return;
			String displayString = def.name();
			int replacementLength = prefix != null ? prefix.length() : 0; 

			ClonkCompletionProposal prop = new ClonkCompletionProposal(def, def.id().stringValue(), offset, replacementLength, def.id().stringValue().length(),
				UI.definitionIcon(def), displayString.trim(), null, null, " - " + def.id().stringValue(), editor()); //$NON-NLS-1$
			prop.setCategory(Category.Definitions);
			proposals.add(prop);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(def.toString());
		}
	}
	
	protected IFile pivotFile() {
		return ((IFileEditorInput)editor.getEditorInput()).getFile();
	}
	
	protected void proposalsForIndexedDefinitions(Index index, int offset, int wordOffset, String prefix, Collection<ICompletionProposal> proposals) {
		for (Definition obj : index.definitionsIgnoringRemoteDuplicates(pivotFile()))
			proposalForDefinition(obj, prefix, wordOffset, proposals);
	}
	
	protected boolean stringMatchesPrefix(String name, String lowercasedPrefix) {
		return name.toLowerCase().contains(lowercasedPrefix);
	}
	
	protected void proposalForFunc(Function func, String prefix, int offset, Collection<ICompletionProposal> proposals, String parentName, boolean brackets) {
		if (prefix != null)
			if (!stringMatchesPrefix(func.name(), prefix))
				return;
		int replacementLength = prefix != null ? prefix.length() : 0;

		String replacement = func.name() + (brackets ? "()" : ""); //$NON-NLS-1$ //$NON-NLS-2$
		ClonkCompletionProposal prop = new ClonkCompletionProposal(
			func, replacement, offset, replacementLength,
			UI.functionIcon(func), null/*contextInformation*/, null, " - " + parentName, editor() //$NON-NLS-1$
		);
		prop.setCategory(Category.Functions);
		proposals.add(prop);
	}
	
	protected ClonkCompletionProposal proposalForVar(Variable var, String prefix, int offset, Collection<ICompletionProposal> proposals) {
		if (prefix != null && !stringMatchesPrefix(var.name(), prefix))
			return null;
		String displayString = var.name();
		int replacementLength = 0;
		if (prefix != null)
			replacementLength = prefix.length();
		ClonkCompletionProposal prop = new ClonkCompletionProposal(
			var,
			var.name(), offset, replacementLength, var.name().length(), UI.variableIcon(var), displayString, 
			null, null, " - " + (var.parentDeclaration() != null ? var.parentDeclaration().name() : "<adhoc>"), //$NON-NLS-1$
			editor()
		);
		prop.setCategory(Category.Variables);
		proposals.add(prop);
		return prop;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}
	
	@Override
	public int compare(ICompletionProposal a, ICompletionProposal b) {
		ClonkCompletionProposal ca = as(a, ClonkCompletionProposal.class);
		ClonkCompletionProposal cb = as(b, ClonkCompletionProposal.class);
		if (ca != null && cb != null) {
			if (prefix != null) {
				class Match {
					boolean startsWith, match, local;
					Match(ClonkCompletionProposal proposal) {
						for (String s : proposal.identifiers())
							if (s.toLowerCase().startsWith(prefix)) {
								startsWith = true;
								if (s.length() == prefix.length()) {
									match = true;
									break;
								}
							} 
						local = proposal.declaration() != null && !proposal.declaration().isGlobal();
					}
				}
				Match ma = new Match(ca), mb = new Match(cb);
				if (ma.match && !mb.match)
					return -1;
				else if (mb.match && !ma.match)
					return +1;
				else if (ma.startsWith && !mb.startsWith)
					return -1;
				else if (mb.startsWith && !ma.startsWith)
					return +1;
			}
			return ca.compareTo(cb);
		}
		return 1;
	}

}
