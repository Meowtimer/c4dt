package net.arctics.clonk.parser.c4script.quickfix;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.Script;
import net.arctics.clonk.ui.editors.c4script.ClonkQuickAssistProcessor;
import net.arctics.clonk.ui.editors.c4script.ClonkQuickAssistProcessor.ParameterizedProposal;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class C4ScriptMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {
	
	public IMarkerResolution[] getResolutions(IMarker marker) {
		ClonkQuickAssistProcessor quickAssist = ClonkQuickAssistProcessor.getSingleton();
		Script script = Script.get(marker.getResource(), true);
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>(10);
		quickAssist.collectProposals(marker, new Position(marker.getAttribute(IMarker.CHAR_START, 0), marker.getAttribute(IMarker.CHAR_END, 0)-marker.getAttribute(IMarker.CHAR_START, 0)), proposals, null, script);
		List<IMarkerResolution> res = new ArrayList<IMarkerResolution>(10);
		for (ICompletionProposal p : proposals) {
			if (p instanceof ParameterizedProposal)
				res.add(new ClonkQuickAssistProcessor.ParameterizedProposalMarkerResolution((ParameterizedProposal) p, marker));
		}
		return res.toArray(new IMarkerResolution[res.size()]);
	}

	public boolean hasResolutions(IMarker marker) {
		try {
			return
				ClonkQuickAssistProcessor.getSingleton() != null &&
				(marker.getType().equals(ClonkCore.MARKER_C4SCRIPT_ERROR) ||
				 marker.getType().equals(ClonkCore.MARKER_C4SCRIPT_ERROR_WHILE_TYPING));
		} catch (CoreException e) {
			return false;
		}
	}

}
