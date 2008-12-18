package net.arctics.clonk.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;

import net.arctics.clonk.resource.c4group.C4Entry;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class SimpleScriptStorage implements IStorage, Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String name;
	private byte[] contents;
	
	public SimpleScriptStorage(C4Entry entry) {
		name = entry.getName();
		contents = entry.getContentsAsArray();
	}

	public InputStream getContents() throws CoreException {
		return new ByteArrayInputStream(contents);
	}

	public IPath getFullPath() {
		return new Path(name);
	}

	public String getName() {
		return name;
	}

	public boolean isReadOnly() {
		return true;
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class cls) {
		return null;
	}

}
