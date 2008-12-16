package net.arctics.clonk.parser;

import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.resource.c4group.C4Group;
import net.arctics.clonk.resource.c4group.C4GroupItem;

public class C4ObjectExtern extends C4Object {

	private static final long serialVersionUID = -4964785375712432236L;
//	protected IPath path;
	protected C4GroupItem script;
	
	public C4ObjectExtern(C4ID id, String name, C4GroupItem script) {
		super(id, name);
		this.script = script;
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
	public Object getScriptFile() {
		return script;
	}

	@Override
	public ClonkIndex getIndex() {
		if (this == ClonkCore.ENGINE_OBJECT)
			return null;
		return ClonkCore.EXTERN_INDEX;
	}

}
