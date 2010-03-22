package net.arctics.clonk.ui.debug;

import net.arctics.clonk.debug.ClonkDebugLineBreakpoint;
import net.arctics.clonk.debug.ClonkDebugStackFrame;
import net.arctics.clonk.debug.ClonkDebugTarget;
import net.arctics.clonk.debug.ClonkDebugThread;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

public class ClonkDebugModelPresentation extends LabelProvider implements IDebugModelPresentation {
	
	public static final String ID = ClonkDebugModelPresentation.class.getName();

	@Override
	public void computeDetail(IValue value, IValueDetailListener listener) {
		try {
			String val = value.getValueString();
			listener.detailComputed(value, val);
		} catch (DebugException e) {
			e.printStackTrace();
			listener.detailComputed(value, "Fail");
		}
	}

	@Override
	public void setAttribute(String attribute, Object value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getEditorId(IEditorInput input, Object element) {
		return "clonk.editors.C4ScriptEditor"; //$NON-NLS-1$
	}

	@Override
	public IEditorInput getEditorInput(Object element) {
		if (element instanceof IFile)
			return new FileEditorInput((IFile) element);
		else if (element instanceof ClonkDebugLineBreakpoint) {
			ClonkDebugLineBreakpoint breakpoint = (ClonkDebugLineBreakpoint) element;
			return getEditorInput(breakpoint.getMarker().getResource());
		}
		return null;
	}
	
	@Override
	public String getText(Object element) {
		try {
			if (element instanceof ClonkDebugThread)
				return ((ClonkDebugThread)element).getName();
			else if (element instanceof ClonkDebugStackFrame)
				return ((ClonkDebugStackFrame) element).getName();
			else if (element instanceof ClonkDebugTarget)
				return ((ClonkDebugTarget) element).getName();
			else if (element instanceof ClonkDebugLineBreakpoint)
				return ((ClonkDebugLineBreakpoint)element).getMarker().getAttribute(IMarker.MESSAGE, "Breakpoint"); //$NON-NLS-1$
			else if (element instanceof IWatchExpression)
				return ((IWatchExpression)element).getExpressionText();
			else
				return "Empty";
		} catch (DebugException e) {
			e.printStackTrace();
			return "Fail"; //$NON-NLS-1$
		}
	}

}
