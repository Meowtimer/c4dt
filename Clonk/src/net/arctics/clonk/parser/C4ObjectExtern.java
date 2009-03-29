package net.arctics.clonk.parser;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.util.INodeWithParent;

public class C4ObjectExtern extends C4Object implements INodeWithParent {

	private static final long serialVersionUID = -4964785375712432236L;
	private SimpleScriptStorage script;
	private INodeWithParent parentNode;
	
	public C4ObjectExtern(C4ID id, String name, C4GroupItem script, INodeWithParent parentNode) {
		super(id, name);
		this.script = script != null ? new SimpleScriptStorage((C4GroupEntry)script) : null;
		this.parentNode = parentNode;
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

	public String nodeName() {
		return getName();
	}

	public INodeWithParent parentNode() {
		return parentNode;
	}

}
