package net.arctics.clonk.ui.editors;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4ObjectExtern;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

public class ObjectExternEditorInput implements IEditorInput, IPathEditorInput, IStorageEditorInput {

	private C4ObjectExtern object;
	
	public ObjectExternEditorInput(C4ObjectExtern object) {
		super();
		this.object = object;
	}

	public boolean exists() {
		return object != null;
	}

	public ImageDescriptor getImageDescriptor() {
		return ClonkCore.getDefault().getIconImageDescriptor("C4Object");
	}

	public String getName() {
		return "[" + object.getName() + "]";
	}

	public IPersistableElement getPersistable() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getToolTipText() {
		return object.getName();
	}

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
		return (obj instanceof ObjectExternEditorInput && ((ObjectExternEditorInput)obj).object == object);
	}

	public IStorage getStorage() throws CoreException {
		return (IStorage)object.getScriptFile();
	}

	public C4Object getObject() {
		return object;
	}

}
