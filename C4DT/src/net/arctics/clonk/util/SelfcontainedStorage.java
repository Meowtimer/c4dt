package net.arctics.clonk.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;

import net.arctics.clonk.Core;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Simple implementation of {@link IStorage} which stores its contents as final string field.
 * @author madeen
 *
 */
public class SelfcontainedStorage implements IStorage, Serializable {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final String name;
	private final String contents;
	public SelfcontainedStorage(String name, String contents) {
		this.name = name;
		this.contents = contents;
	}
	@Override
	public InputStream getContents() throws CoreException { return new ByteArrayInputStream(contents.getBytes()); }
	public String contentsAsString() { return contents; }
	@Override
	public IPath getFullPath() { return new Path(name); }
	@Override
	public String getName() { return name; }
	@Override
	public boolean isReadOnly() { return true; }
	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class cls) { return null; }
}
