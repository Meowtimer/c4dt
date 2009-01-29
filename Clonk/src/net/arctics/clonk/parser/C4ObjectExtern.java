package net.arctics.clonk.parser;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.resource.c4group.C4Entry;
import net.arctics.clonk.resource.c4group.C4GroupItem;

public class C4ObjectExtern extends C4Object {

	private static final long serialVersionUID = -4964785375712432236L;
	protected SimpleScriptStorage script;
	
	public C4ObjectExtern(C4ID id, String name, C4GroupItem script) {
		super(id, name);
		this.script = script != null ? new SimpleScriptStorage((C4Entry)script) : null;
	}

	@Override
	public Object getScriptFile() {
		return script;
	}

	@Override
	public ClonkIndex getIndex() {
		if (this == ClonkCore.getDefault().ENGINE_OBJECT)
			return null;
		return ClonkCore.getDefault().EXTERN_INDEX;
	}

}
