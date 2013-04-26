package net.arctics.clonk.c4script.quickfix;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.ProblemReportingStrategy;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.ui.editors.c4script.ScriptQuickAssistProcessor;
import net.arctics.clonk.ui.editors.c4script.ScriptQuickAssistProcessor.ParameterizedProposal;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class C4ScriptMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {
	
	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		ScriptQuickAssistProcessor quickAssist = ScriptQuickAssistProcessor.singleton();
		Script script = Script.get(marker.getResource(), true);
		List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>(10);
		quickAssist.collectProposals(marker, new Position(marker.getAttribute(IMarker.CHAR_START, 0), marker.getAttribute(IMarker.CHAR_END, 0)-marker.getAttribute(IMarker.CHAR_START, 0)),
			proposals, null, script, script.index().nature().instantiateProblemReportingStrategies(ProblemReportingStrategy.Capabilities.TYPING).get(0).localReporter(script, 0, null));
		List<IMarkerResolution> res = new ArrayList<IMarkerResolution>(10);
		for (ICompletionProposal p : proposals)
			if (p instanceof ParameterizedProposal)
				res.add(new ScriptQuickAssistProcessor.ParameterizedProposalMarkerResolution((ParameterizedProposal) p, marker));
		return res.toArray(new IMarkerResolution[res.size()]);
	}

	@Override
	public boolean hasResolutions(IMarker marker) {
		try {
			return
				ScriptQuickAssistProcessor.singleton() != null &&
				(marker.getType().equals(Core.MARKER_C4SCRIPT_ERROR) ||
				 marker.getType().equals(Core.MARKER_C4SCRIPT_ERROR_WHILE_TYPING));
		} catch (CoreException e) {
			return false;
		}
	}

}