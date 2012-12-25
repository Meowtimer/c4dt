package net.arctics.clonk.ui.editors;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal.Category;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.ui.IFileEditorInput;

public abstract class ClonkCompletionProcessor<EditorType extends ClonkTextEditor> implements IContentAssistProcessor {

	public EditorType editor() {
		return editor;
	}

	protected EditorType editor;
	
	public ClonkCompletionProcessor(EditorType editor) {
		this.editor = editor;
	}
	
	protected void proposalForDefinition(Definition def, String prefix, int offset, Collection<ICompletionProposal> proposals) {
		try {
			if (def == null || def.id() == null)
				return;

			if (prefix != null)
				if (!(
					def.name().toLowerCase().contains(prefix) ||
					def.id().stringValue().toLowerCase().contains(prefix) ||
					// also check if the user types in the folder name
					(def instanceof Definition && def.definitionFolder() != null && def.definitionFolder().getName().contains(prefix))
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
	
	protected void proposalForFunc(Function func, String prefix, int offset, Collection<ICompletionProposal> proposals, String parentName, boolean brackets) {
		if (prefix != null)
			if (!func.name().toLowerCase().startsWith(prefix))
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
		if (prefix != null && !var.name().toLowerCase().contains(prefix))
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

	protected ICompletionProposal[] sortProposals(Collection<ICompletionProposal> proposals) {
		ICompletionProposal[] arr = proposals.toArray(new ICompletionProposal[proposals.size()]);
		Arrays.sort(arr, new Comparator<ICompletionProposal>() {
			@Override
			public int compare(ICompletionProposal a, ICompletionProposal b) {
				if (a instanceof ClonkCompletionProposal && b instanceof ClonkCompletionProposal)
					return ((ClonkCompletionProposal)a).compareTo((ClonkCompletionProposal)b);
				return 1;
			}
		});
		return arr;
	}
	
	@Override
	public String getErrorMessage() {
		return null;
	}

}
