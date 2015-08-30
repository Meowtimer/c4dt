package net.arctics.clonk.debug;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;

import net.arctics.clonk.ui.editors.c4script.C4ScriptEditor;

public class BreakpointAdapterFactory implements IAdapterFactory {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(final Object adaptableObject, final Class adapterType) {
		if (adaptableObject instanceof C4ScriptEditor) {
			final IResource resource = ((C4ScriptEditor)adaptableObject).getEditorInput().getAdapter(IResource.class);
			if (resource != null) {
				return new BreakpointAdapter();
			}
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Class[] getAdapterList() {
		return new Class[] {
			C4ScriptEditor.class
		};
	}

}