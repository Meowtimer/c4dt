package net.arctics.clonk.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;

public class SourceLookupParticipant extends AbstractSourceLookupParticipant {
	@Override
	public String getSourceName(final Object object) throws CoreException {
		if (object instanceof StackFrame) {
			final StackFrame stackFrame = (StackFrame) object;
			return stackFrame.getSourcePath();
		} else {
			return null;
		}
	}
}
