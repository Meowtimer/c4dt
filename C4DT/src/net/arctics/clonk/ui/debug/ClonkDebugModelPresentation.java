package net.arctics.clonk.ui.debug;

import net.arctics.clonk.index.IExternalScript;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.ui.editors.c4script.ScriptWithStorageEditorInput;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

public class ClonkDebugModelPresentation extends LabelProvider implements IDebugModelPresentation {

	@Override
	public void computeDetail(IValue value, IValueDetailListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAttribute(String attribute, Object value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getEditorId(IEditorInput input, Object element) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IEditorInput getEditorInput(Object element) {
		if (element instanceof IFile)
			return new FileEditorInput((IFile) element);
		else if (element instanceof IExternalScript)
			return new ScriptWithStorageEditorInput((C4ScriptBase)element);
		return null;
	}

}
