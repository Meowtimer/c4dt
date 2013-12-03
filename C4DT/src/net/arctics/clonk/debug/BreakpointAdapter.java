package net.arctics.clonk.debug;

import net.arctics.clonk.ui.debug.ClonkDebugModelPresentation;
import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

public class BreakpointAdapter implements IToggleBreakpointsTarget {

	@Override
	public void toggleLineBreakpoints(final IWorkbenchPart part, final ISelection selection)  throws CoreException {
		final ITextEditor textEditor = (ITextEditor)part;
		if (textEditor != null) {
			final IResource resource = (IResource) textEditor.getEditorInput().getAdapter(IResource.class);
			final ITextSelection textSelection = (ITextSelection) selection;
			final int lineNumber = textSelection.getStartLine();
			final IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(ClonkDebugModelPresentation.ID);
			for (int i = 0; i < breakpoints.length; i++) {
				final IBreakpoint breakpoint = breakpoints[i];
				if (resource.equals(breakpoint.getMarker().getResource()))
					if (((ILineBreakpoint)breakpoint).getLineNumber() == (lineNumber + 1)) {
						breakpoint.delete();
						return;
					}
			}
			final Breakpoint lineBreakpoint = new Breakpoint(resource, lineNumber + 1);
			DebugPlugin.getDefault().getBreakpointManager().addBreakpoint(lineBreakpoint);
		}
	}
	
	@Override
	public boolean canToggleLineBreakpoints(final IWorkbenchPart part, final ISelection selection) {
		return part instanceof C4ScriptEditor;
	}

	@Override
	public void toggleMethodBreakpoints(final IWorkbenchPart part, final ISelection selection) throws CoreException {
	}

	@Override
	public boolean canToggleMethodBreakpoints(final IWorkbenchPart part, final ISelection selection) {
		return false;
	}

	@Override
	public void toggleWatchpoints(final IWorkbenchPart part, final ISelection selection) throws CoreException {
	}

	@Override
	public boolean canToggleWatchpoints(final IWorkbenchPart part, final ISelection selection) {
		return false;
	}

}