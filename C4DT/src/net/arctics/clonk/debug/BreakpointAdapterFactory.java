package net.arctics.clonk.debug;

import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;

public class BreakpointAdapterFactory implements IAdapterFactory {

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(final Object adaptableObject, final Class adapterType) {
		if (adaptableObject instanceof C4ScriptEditor) {
			final IResource resource = (IResource) ((C4ScriptEditor)adaptableObject).getEditorInput().getAdapter(IResource.class);
			if (resource != null)
				return new BreakpointAdapter(); 
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class[] getAdapterList() {
		return new Class[] {
			C4ScriptEditor.class
		};
	}

}