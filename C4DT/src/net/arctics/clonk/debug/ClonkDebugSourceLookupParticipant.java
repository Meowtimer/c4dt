package net.arctics.clonk.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;

public class ClonkDebugSourceLookupParticipant extends AbstractSourceLookupParticipant {

	@Override
	public String getSourceName(Object object) throws CoreException {
		if (object instanceof ClonkDebugStackFrame) {
			ClonkDebugStackFrame stackFrame = (ClonkDebugStackFrame) object;
			return stackFrame.getName();
		}
		else
			return null;
	}

}
