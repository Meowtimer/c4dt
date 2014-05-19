package net.arctics.clonk.ui.debug;

import static net.arctics.clonk.util.Utilities.block;
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
	public void computeDetail(final IValue value, final IValueDetailListener listener) {
		try {
			final String val = value.getValueString();
			listener.detailComputed(value, val);
		} catch (final DebugException e) {
			e.printStackTrace();
			listener.detailComputed(value, "Fail"); //$NON-NLS-1$
		}
	}

	@Override
	public void setAttribute(final String attribute, final Object value) {}

	@Override
	public String getEditorId(final IEditorInput input, final Object element) {
		return "clonk.editors.C4ScriptEditor"; //$NON-NLS-1$
	}

	@Override
	public IEditorInput getEditorInput(final Object element) {
		if (element instanceof IFile)
			return new FileEditorInput((IFile) element);
		else if (element instanceof Breakpoint) {
			final Breakpoint breakpoint = (Breakpoint) element;
			return getEditorInput(breakpoint.getMarker().getResource());
		}
		return null;
	}

	@Override
	public String getText(final Object element) {
		return
			element instanceof ScriptThread ? ((ScriptThread)element).getName() :
			element instanceof StackFrame ? ((StackFrame) element).getName() :
			element instanceof Target ? ((Target) element).getName() :
			element instanceof Breakpoint ? ((Breakpoint)element).getMarker().getAttribute(IMarker.MESSAGE, "Breakpoint") : //$NON-NLS-1$
			element instanceof IWatchExpression ? block(() -> {
				final IWatchExpression expr = (IWatchExpression) element;
				try {
					return expr.getExpressionText() + " == " + (expr.getValue() != null ? expr.getValue().getValueString() : "<nil>");
				} catch (final Exception e) {
					e.printStackTrace();
					return "Fail";
				} //$NON-NLS-1$ //$NON-NLS-2$
			}) :
			"Empty"; //$NON-NLS-1$
	}

}
