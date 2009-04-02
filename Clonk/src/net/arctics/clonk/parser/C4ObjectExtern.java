package net.arctics.clonk.parser;

import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.util.INodeWithParent;

public class C4ObjectExtern extends C4Object implements INodeWithParent {

	private static final long serialVersionUID = -4964785375712432236L;
	
	private SimpleScriptStorage script;
	private INodeWithParent parentNode;
	private String nodeName;
	
	public C4ObjectExtern(C4ID id, String name, C4GroupItem script, INodeWithParent parentNode) {
		super(id, name);
		this.script = script != null ? new SimpleScriptStorage((C4GroupEntry)script) : null;
		this.parentNode = parentNode;
		this.nodeName = script.getParentGroup().getName();
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

	// return name of folder instead of name of object (so constructed paths don't look wierd)
	public String nodeName() {
		if (nodeName == null)
			nodeName = getName();
		return nodeName;
	}

	public INodeWithParent parentNode() {
		return parentNode;
	}

	public IPath getPath() {
		return INodeWithParent.Default.getPath(this); 
	}

	public boolean subNodeOf(INodeWithParent node) {
		return INodeWithParent.Default.subNodeOf(this, node);
	}

}
