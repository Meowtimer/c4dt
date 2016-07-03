package net.arctics.clonk.ui.editors.c4script;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

import net.arctics.clonk.parser.Markers;


public final class ParameterizedProposalMarkerResolution extends WorkbenchMarkerResolution {
	private final ParameterizedProposal proposal;
	private final IMarker originalMarker;
	public ParameterizedProposalMarkerResolution(final ParameterizedProposal proposal, final IMarker originalMarker) {
		this.proposal = proposal;
		this.originalMarker = originalMarker;
	}
	@Override
	public String getDescription() { return proposal.getDisplayString(); }
	@Override
	public Image getImage() { return proposal.getImage(); }
	@Override
	public String getLabel() { return proposal.getDisplayString(); }
	@Override
	public void run(final IMarker marker) { proposal.runOnMarker(marker); }
	private boolean relevant(final IMarker marker) {
		return
			!marker.equals(this.originalMarker) &&
			Markers.problem(marker) == Markers.problem(originalMarker);
	}
	@Override
	public IMarker[] findOtherMarkers(final IMarker[] markers) {
		final List<IMarker> result = new ArrayList<IMarker>(markers.length);
		for (final IMarker m : markers)
			if (relevant(m))
				result.add(m);
		return result.toArray(new IMarker[result.size()]);
	}
}