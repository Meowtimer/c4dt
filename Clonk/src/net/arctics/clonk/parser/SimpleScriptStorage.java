package net.arctics.clonk.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import net.arctics.clonk.preferences.PreferenceConstants;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class SimpleScriptStorage implements IStorage, Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String name;
	private String contents;
	
	public SimpleScriptStorage(C4GroupEntry entry) {
		name = entry.getName();
		try {
			contents = new String(entry.getContentsAsArray(), Utilities.getPreference(PreferenceConstants.EXTERNAL_INDEX_ENCODING, PreferenceConstants.EXTERNAL_INDEX_ENCODING_DEFAULT, null));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			contents = "";
		}
	}
	
	public SimpleScriptStorage(String name, String contents) throws UnsupportedEncodingException {
		this.name = name;
		this.contents = contents;
	}

	public InputStream getContents() throws CoreException {
		try {
			return new ByteArrayInputStream(contents.getBytes(Utilities.getPreference(PreferenceConstants.EXTERNAL_INDEX_ENCODING, PreferenceConstants.EXTERNAL_INDEX_ENCODING_DEFAULT, null)));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String getContentsAsString() {
		return contents;
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
