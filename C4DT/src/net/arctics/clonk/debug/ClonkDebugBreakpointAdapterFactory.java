package net.arctics.clonk.debug;

import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;

public class ClonkDebugBreakpointAdapterFactory implements IAdapterFactory {

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof C4ScriptEditor) {
			IResource resource = (IResource) ((C4ScriptEditor)adaptableObject).getEditorInput().getAdapter(IResource.class);
			if (resource != null) {
				return new ClonkDebugBreakpointAdapter();
			} 
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class[] getAdapterList() {
		return new Class[] {
			ClonkDebugBreakpointAdapter.class
		};
	}

}