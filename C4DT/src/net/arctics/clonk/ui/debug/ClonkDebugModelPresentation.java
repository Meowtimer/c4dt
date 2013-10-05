package net.arctics.clonk.ui.debug;

import net.arctics.clonk.debug.Breakpoint;
import net.arctics.clonk.debug.ScriptThread;
import net.arctics.clonk.debug.StackFrame;
import net.arctics.clonk.debug.Target;

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
			final String val = value.getValueString();
			listener.detailComputed(value, val);
		} catch (final DebugException e) {
			e.printStackTrace();
			listener.detailComputed(value, "Fail"); //$NON-NLS-1$
		}
	}

	@Override
	public void setAttribute(String attribute, Object value) {}

	@Override
	public String getEditorId(IEditorInput input, Object element) {
		return "clonk.editors.C4ScriptEditor"; //$NON-NLS-1$
	}

	@Override
	public IEditorInput getEditorInput(Object element) {
		if (element instanceof IFile)
			return new FileEditorInput((IFile) element);
		else if (element instanceof Breakpoint) {
			final Breakpoint breakpoint = (Breakpoint) element;
			return getEditorInput(breakpoint.getMarker().getResource());
		}
		return null;
	}
	
	@Override
	public String getText(Object element) {
		try {
			if (element instanceof ScriptThread)
				return ((ScriptThread)element).getName();
			else if (element instanceof StackFrame)
				return ((StackFrame) element).getName();
			else if (element instanceof Target)
				return ((Target) element).getName();
			else if (element instanceof Breakpoint)
				return ((Breakpoint)element).getMarker().getAttribute(IMarker.MESSAGE, "Breakpoint"); //$NON-NLS-1$
			else if (element instanceof IWatchExpression) {
				final IWatchExpression expr = (IWatchExpression) element;
				return expr.getExpressionText() + " == " + (expr.getValue() != null ? expr.getValue().getValueString() : "<nil>"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			else
				return "Empty"; //$NON-NLS-1$
		} catch (final DebugException e) {
			e.printStackTrace();
			return "Fail"; //$NON-NLS-1$
		}
	}

}
