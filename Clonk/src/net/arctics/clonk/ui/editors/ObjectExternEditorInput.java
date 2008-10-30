package net.arctics.clonk.ui.editors;

import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4ObjectExtern;
import net.arctics.clonk.resource.c4group.C4GroupItem;

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
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		return object.getName();
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
		// TODO Auto-generated method stub
		return null;
	}
	@Override

	public boolean equals(Object obj) {
		return (obj instanceof ObjectExternEditorInput && ((ObjectExternEditorInput)obj).object == object);
	}

	public IStorage getStorage() throws CoreException {
		C4GroupItem scriptEntry = object.getGroup().findEntry("Script.c");
		return (IStorage)scriptEntry;
	}

	public C4Object getObject() {
		return object;
	}

}
