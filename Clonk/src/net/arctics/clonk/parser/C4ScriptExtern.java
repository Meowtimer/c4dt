package net.arctics.clonk.parser;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.resource.c4group.C4GroupItem;

public class C4ScriptExtern extends C4ScriptBase implements INodeWithParent {
	
	private static final long serialVersionUID = 1L;
	
	private SimpleScriptStorage scriptStorage;
	private INodeWithParent parentNode;

	public C4ScriptExtern(C4GroupItem script, INodeWithParent parentNode) {
		this.parentNode = parentNode;
		setName(script.getName());
		scriptStorage = new SimpleScriptStorage((C4GroupEntry) script);
	}
	
	@Override
	public ClonkIndex getIndex() {
		return ClonkCore.getDefault().EXTERN_INDEX;
	}

	@Override
	public Object getScriptFile() {
		return scriptStorage;
	}

	public String nodeName() {
		return getName();
	}

	public INodeWithParent parentNode() {
		return parentNode;
	}

}
