package net.arctics.clonk.parser;

import java.util.List;

import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4GroupItem;

import org.eclipse.core.runtime.IPath;

public class C4ObjectExtern extends C4Object {

	private static final long serialVersionUID = -4964785375712432236L;
//	protected IPath path;
	protected C4Group group;
	
	public C4ObjectExtern(C4ID id, String name, C4Group group) {
		super(id, name);
//		this.path = path;
		this.group = group;
	}

	/**
	 * Clonk directory related path.
	 * @return the path
	 */
//	public IPath getPath() {
//		return path;
//	}

	/**
	 * Set a new path.<br><br>
	 * <b>Notice:</b> This path has to be Clonk-directory-related
	 * @param path the path to set
	 */
//	public void setPath(IPath path) {
//		this.path = path;
//	}

	@Override
	public Object getScript() {
		if (group == null) return null;
		List<C4GroupItem> children = group.getChildEntries();
		for(C4GroupItem item : children) {
			if (item.getName().endsWith(".c")) {
				return item;
			}
		}
		// TODO C4ObjectExtern has to provide a method to get the Script.c
		return null;
	}
	
	

}
