package net.arctics.clonk.ui.editors;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import net.arctics.clonk.index.C4Object;
import net.arctics.clonk.index.C4ObjectIntern;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.c4script.C4Function;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.ui.IFileEditorInput;

public abstract class ClonkCompletionProcessor<EditorType extends ClonkTextEditor> implements IContentAssistProcessor {

	public EditorType getEditor() {
		return editor;
	}

	protected EditorType editor;
	
	public ClonkCompletionProcessor(EditorType editor) {
		this.editor = editor;
	}
	
	protected void proposalForObject(C4Object obj,String prefix,int offset,Collection<ICompletionProposal> proposals) {
		try {
			if (obj == null || obj.getId() == null)
				return;

			if (prefix != null) {
				if (!(
					obj.getName().toLowerCase().startsWith(prefix) ||
					obj.getId().getName().toLowerCase().startsWith(prefix) ||
					(obj instanceof C4ObjectIntern && ((C4ObjectIntern)obj).getObjectFolder() != null && ((C4ObjectIntern)obj).getObjectFolder().getName().startsWith(prefix))
				))
					return;
			}
			String displayString = obj.getName();
			int replacementLength = prefix != null ? prefix.length() : 0;

			// no need for context information
//			String contextInfoString = obj.getName();
//			IContextInformation contextInformation = null;// new ContextInformation(obj.getId().getName(),contextInfoString); 

			ICompletionProposal prop = new ClonkCompletionProposal(obj.getId().getName(), offset, replacementLength, obj.getId().getName().length(),
					Utilities.getIconForObject(obj), displayString.trim(), null, null, " - " + obj.getId().getName());
			proposals.add(prop);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(obj.toString());
		}
	}
	
	protected void proposalsForIndexedObjects(ClonkIndex index, int offset, int wordOffset, String prefix, Collection<ICompletionProposal> proposals) {
		for (C4Object obj : index.objectsIgnoringRemoteDuplicates(((IFileEditorInput)editor.getEditorInput()).getFile())) {
			proposalForObject(obj, prefix, wordOffset, proposals);
		}
	}
	
	protected void proposalForFunc(C4Function func,String prefix,int offset, Collection<ICompletionProposal> proposals,String parentName, boolean brackets) {
		if (prefix != null) {
			if (!func.getName().toLowerCase().startsWith(prefix))
				return;
		}
		String displayString = func.getLongParameterString(true);
		int replacementLength = 0;
		if (prefix != null) replacementLength = prefix.length();
		
		String contextInfoString = func.getLongParameterString(false);
		IContextInformation contextInformation = new ContextInformation(func.getName() + "()",contextInfoString); 
		
		String replacement = func.getName() + (brackets ? "()" : "");
		ClonkCompletionProposal prop = new ClonkCompletionProposal(replacement, offset,replacementLength,func.getName().length()+1,
				Utilities.getIconForFunction(func), displayString.trim(),contextInformation, func.getShortInfo()," - " + parentName);
		proposals.add(prop);
	}

	protected ICompletionProposal[] sortProposals(Collection<ICompletionProposal> proposals) {
		ICompletionProposal[] arr = proposals.toArray(new ICompletionProposal[proposals.size()]);
		Arrays.sort(arr, new Comparator<ICompletionProposal>() {
			public int compare(ICompletionProposal propA, ICompletionProposal propB) {
				String dispA = propA.getDisplayString(), dispB = propB.getDisplayString();
				if (dispA.startsWith("[")) {
					if (!dispB.startsWith("[")) {
						return 1;
					}
				}
				else if (dispB.startsWith("[")) {
					if (!dispA.startsWith("[")) {
						return -1;
					}
				}
				return dispA.compareToIgnoreCase(dispB);
			}
		});
		return arr;
	}
	
	public String getErrorMessage() {
		return null;
	}

}
