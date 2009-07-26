package net.arctics.clonk.parser.c4script.quickfix;

import net.arctics.clonk.ClonkCore;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class C4ScriptMarkerResolutionGenerator implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {
	
	public IMarkerResolution[] getResolutions(IMarker marker) {
		return new IMarkerResolution[] {
			new C4ScriptMarkerResolution(marker)
		};
	}

	public boolean hasResolutions(IMarker marker) {
		try {
			return marker.getType().equals(ClonkCore.MARKER_C4SCRIPT_ERROR);
		} catch (CoreException e) {
			return false;
		}
	}

}
