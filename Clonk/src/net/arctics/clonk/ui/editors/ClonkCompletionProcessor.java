package net.arctics.clonk.ui.editors;

import java.util.Collection;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4ObjectIntern;
import net.arctics.clonk.parser.ClonkIndex;
import net.arctics.clonk.ui.editors.c4script.ClonkCompletionProposal;
import net.arctics.clonk.util.Utilities;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

public abstract class ClonkCompletionProcessor implements IContentAssistProcessor {

	protected ITextEditor editor;
	
	public ClonkCompletionProcessor(ITextEditor editor) {
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

}
