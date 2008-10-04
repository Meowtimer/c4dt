package net.arctics.clonk.parser;

import org.eclipse.core.runtime.IPath;

public class C4ObjectExtern extends C4Object {

	protected IPath path;
	
	public C4ObjectExtern(C4ID id, String name, IPath path) {
		super(id, name);
		this.path = path;
	}

	/**
	 * Clonk directory related path.
	 * @return the path
	 */
	public IPath getPath() {
		return path;
	}

	/**
	 * Set a new path.<br><br>
	 * <b>Notice:</b> This path has to be Clonk-directory-related
	 * @param path the path to set
	 */
	public void setPath(IPath path) {
		this.path = path;
	}

	@Override
	public Object getScript() {
		// TODO C4ObjectExtern has to provide a method to get the Script.c
		return null;
	}
	
	

}
