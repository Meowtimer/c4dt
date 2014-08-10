package net.arctics.clonk.c4script.quickfix;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.Core;
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
	public IMarkerResolution[] getResolutions(final IMarker marker) {
		final ScriptQuickAssistProcessor quickAssist = ScriptQuickAssistProcessor.singleton();
		final Script script = Script.get(marker.getResource(), true);
		final List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>(10);
		quickAssist.collectProposals(marker, new Position(marker.getAttribute(IMarker.CHAR_START, 0), marker.getAttribute(IMarker.CHAR_END, 0)-marker.getAttribute(IMarker.CHAR_START, 0)),
			proposals, null, script);
		final List<IMarkerResolution> res = new ArrayList<IMarkerResolution>(10);
		for (final ICompletionProposal p : proposals)
			if (p instanceof ParameterizedProposal)
				res.add(new ScriptQuickAssistProcessor.ParameterizedProposalMarkerResolution((ParameterizedProposal) p, marker));
		return res.toArray(new IMarkerResolution[res.size()]);
	}
	@Override
	public boolean hasResolutions(final IMarker marker) {
		try {
			return
				ScriptQuickAssistProcessor.singleton() != null &&
				marker.getType().equals(Core.MARKER_C4SCRIPT_ERROR);
		} catch (final CoreException e) {
			return false;
		}
	}
}
