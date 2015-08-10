package net.arctics.clonk.ui.editors.c4script;

import java.lang.ref.WeakReference;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.util.ITreeNode;

public class ScriptWithStorageEditorInput extends PlatformObject implements IEditorInput, IPathEditorInput, IStorageEditorInput, IPersistableElement {

	private static final String FACTORY_ID = Core.id("ui.editors.scriptWithStorageEditorInputFactory");   //$NON-NLS-1$

	private final WeakReference<Script> script;

	public ScriptWithStorageEditorInput(final Script script) {
		super();

		if (!(script.source() instanceof IStorage))
			throw new IllegalArgumentException("script"); //$NON-NLS-1$
		this.script = new WeakReference<Script>(script);
	}

	@Override
	public boolean exists() { return script != null && script.get() != null; }
	@Override
	public ImageDescriptor getImageDescriptor() { return Core.instance().iconImageDescriptorFor("C4Object"); } //$NON-NLS-1$

	@Override
	public String getName() {
		return "[" + script().name() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public IPersistableElement getPersistable() {
		return this;
	}

	@Override
	public String getToolTipText() {
		return ((ITreeNode)script()).path().toOSString();
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getAdapter(final Class cls) {
		return null;
	}

	@Override
	public IPath getPath() {
		try {
			if (script instanceof ITreeNode)
				return ((ITreeNode)script).path();
			return getStorage().getFullPath();
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean equals(final Object obj) {
		return (obj instanceof ScriptWithStorageEditorInput && ((ScriptWithStorageEditorInput)obj).script() == script());
	}

	@Override
	public IStorage getStorage() throws CoreException {
		return script().source();
	}

	public Script script() {
		return script.get();
	}

	@Override
	public String getFactoryId() {
		return FACTORY_ID;
	}

	@Override
	public void saveState(final IMemento memento) {
		memento.putString("path", getPath().toPortableString()); //$NON-NLS-1$
	}

}
