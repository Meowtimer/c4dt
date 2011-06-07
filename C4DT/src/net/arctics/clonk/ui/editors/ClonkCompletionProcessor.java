package net.arctics.clonk.ui.editors;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.ProjectDefinition;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.Variable;
import net.arctics.clonk.ui.editors.ClonkCompletionProposal.Category;
import net.arctics.clonk.util.UI;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.ui.IFileEditorInput;

public abstract class ClonkCompletionProcessor<EditorType extends ClonkTextEditor> implements IContentAssistProcessor {

	public EditorType getEditor() {
		return editor;
	}

	protected EditorType editor;
	
	public ClonkCompletionProcessor(EditorType editor) {
		this.editor = editor;
	}
	
	protected void proposalForDefinition(Definition obj,String prefix,int offset,Collection<ICompletionProposal> proposals) {
		try {
			if (obj == null || obj.id() == null)
				return;

			if (prefix != null) {
				if (!(
					obj.getName().toLowerCase().contains(prefix) ||
					obj.id().getName().toLowerCase().contains(prefix) ||
					// also check if the user types in the folder name
					(obj instanceof ProjectDefinition && ((ProjectDefinition)obj).definitionFolder() != null && ((ProjectDefinition)obj).definitionFolder().getName().contains(prefix))
				))
					return;
			}
			String displayString = obj.getName();
			int replacementLength = prefix != null ? prefix.length() : 0; 

			ClonkCompletionProposal prop = new ClonkCompletionProposal(obj, obj.id().getName(), offset, replacementLength, obj.id().getName().length(),
				UI.getIconForObject(obj), displayString.trim(), null, obj.getInfoText(), " - " + obj.id().getName(), getEditor()); //$NON-NLS-1$
			prop.setCategory(Category.Definitions);
			proposals.add(prop);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(obj.toString());
		}
	}
	
	protected IFile pivotFile() {
		return ((IFileEditorInput)editor.getEditorInput()).getFile();
	}
	
	protected void proposalsForIndexedDefinitions(ClonkIndex index, int offset, int wordOffset, String prefix, Collection<ICompletionProposal> proposals) {
		for (Definition obj : index.objectsIgnoringRemoteDuplicates(pivotFile())) {
			proposalForDefinition(obj, prefix, wordOffset, proposals);
		}
	}
	
	protected void proposalForFunc(Function func,String prefix,int offset, Collection<ICompletionProposal> proposals,String parentName, boolean brackets) {
		if (prefix != null) {
			if (!func.getName().toLowerCase().startsWith(prefix))
				return;
		}
		String displayString = func.getLongParameterString(true);
		int replacementLength = 0;
		if (prefix != null) replacementLength = prefix.length();
		
		/*String contextInfoString = func.getLongParameterString(false);
		IContextInformation contextInformation = new ContextInformation(func.getName() + "()",contextInfoString);  //$NON-NLS-1$*/

		String replacement = func.getName() + (brackets ? "()" : ""); //$NON-NLS-1$ //$NON-NLS-2$
		int cursorPosition = func.getName().length();
		if (brackets) {
			if (func.getParameters().size() == 0)
				cursorPosition += 2;
			else
				cursorPosition++;
		}
		ClonkCompletionProposal prop = new ClonkCompletionProposal(func, replacement, offset,replacementLength,cursorPosition,
				UI.getIconForFunction(func), displayString.trim(), null/*contextInformation*/, null," - " + parentName, getEditor()); //$NON-NLS-1$
		prop.setCategory(Category.Functions);
		proposals.add(prop);
	}
	
	protected ClonkCompletionProposal proposalForVar(Variable var, String prefix, int offset, Collection<ICompletionProposal> proposals) {
		if (prefix != null && !var.getName().toLowerCase().contains(prefix))
			return null;
		if (var.getIndex() == null)
			return null;
		String displayString = var.getName();
		int replacementLength = 0;
		if (prefix != null)
			replacementLength = prefix.length();
		ClonkCompletionProposal prop = new ClonkCompletionProposal(var,
			var.getName(), offset, replacementLength, var.getName().length(), UI.getIconForVariable(var), displayString, 
			null, var.getInfoText(), " - " + (var.getScript() != null ? var.getScript().getName() : "<adhoc>"), //$NON-NLS-1$
			getEditor()
		);
		prop.setCategory(Category.Variables);
		proposals.add(prop);
		return prop;
	}

	protected ICompletionProposal[] sortProposals(Collection<ICompletionProposal> proposals) {
		ICompletionProposal[] arr = proposals.toArray(new ICompletionProposal[proposals.size()]);
		Arrays.sort(arr, new Comparator<ICompletionProposal>() {
			public int compare(ICompletionProposal a, ICompletionProposal b) {
				if (a instanceof ClonkCompletionProposal && b instanceof ClonkCompletionProposal) {
					return ((ClonkCompletionProposal)a).compareTo((ClonkCompletionProposal)b);
				}
				return 1;
			}
		});
		return arr;
	}
	
	public String getErrorMessage() {
		return null;
	}

}
