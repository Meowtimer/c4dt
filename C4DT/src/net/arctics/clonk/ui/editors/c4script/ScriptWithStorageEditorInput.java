package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4ScriptBase;
import net.arctics.clonk.util.ITreeNode;

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

public class ScriptWithStorageEditorInput extends PlatformObject implements IEditorInput, IPathEditorInput, IStorageEditorInput, IPersistableElement {

	private static final String FACTORY_ID = ClonkCore.id("ui.editors.scriptWithStorageEditorInputFactory");   //$NON-NLS-1$
	
	private C4ScriptBase script;
	
	public ScriptWithStorageEditorInput(C4ScriptBase script) {
		super();
		
		if (!(script.getScriptFile() instanceof IStorage))
			throw new IllegalArgumentException("script"); //$NON-NLS-1$
		this.script = script;
	}

	public boolean exists() {
		return script != null;
	}

	public ImageDescriptor getImageDescriptor() {
		return ClonkCore.getDefault().getIconImageDescriptor("C4Object"); //$NON-NLS-1$
	}

	public String getName() {
		return "[" + script.getName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IPersistableElement getPersistable() {
		return this;
	}

	public String getToolTipText() {
		return ((ITreeNode)script).getPath().toOSString();
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class cls) {
		return null;
	}

	public IPath getPath() {
		try {
			if (script instanceof ITreeNode)
				return ((ITreeNode)script).getPath();
			return getStorage().getFullPath();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof ScriptWithStorageEditorInput && ((ScriptWithStorageEditorInput)obj).script == script);
	}

	public IStorage getStorage() throws CoreException {
		return (IStorage)script.getScriptFile();
	}

	public C4ScriptBase getScript() {
		return script;
	}

	public String getFactoryId() {
		return FACTORY_ID;
	}

	public void saveState(IMemento memento) {
		memento.putString("path", getPath().toPortableString()); //$NON-NLS-1$
	}

}
