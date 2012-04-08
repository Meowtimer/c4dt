package net.arctics.clonk.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import net.arctics.clonk.Core;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class SimpleScriptStorage implements IStorage, Serializable {
	
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private String name;
	private String contents;
	
	public SimpleScriptStorage(String name, String contents) throws UnsupportedEncodingException {
		this.name = name;
		this.contents = contents;
	}

	@Override
	public InputStream getContents() throws CoreException {
		return new ByteArrayInputStream(contents.getBytes());
	}
	
	public String getContentsAsString() {
		return contents;
	}

	@Override
	public IPath getFullPath() {
		return new Path(name);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class cls) {
		return null;
	}

}
