package net.arctics.clonk.ui.editors.c4script;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4ScriptBase;
import net.arctics.clonk.util.ITreeNode;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

public class ScriptWithStorageEditorInput implements IEditorInput, IPathEditorInput, IStorageEditorInput {

	private C4ScriptBase script;
	
	public ScriptWithStorageEditorInput(C4ScriptBase object) {
		super();
		this.script = object;
	}

	public boolean exists() {
		return script != null;
	}

	public ImageDescriptor getImageDescriptor() {
		return ClonkCore.getDefault().getIconImageDescriptor("C4Object");
	}

	public String getName() {
		return "[" + script.getName() + "]";
	}

	public IPersistableElement getPersistable() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getToolTipText() {
		return ((ITreeNode)script).getPath().toOSString();
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class cls) {
		// TODO Auto-generated method stub
		return null;
	}

	public IPath getPath() {
		try {
			return getStorage().getFullPath();
		} catch (Exception e) {
			// TODO Auto-generated catch block
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

}
