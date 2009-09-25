package net.arctics.clonk.parser.c4script;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.util.ITreeNode;

public class C4ScriptExtern extends C4ScriptBase {
	
	private static final long serialVersionUID = 1L;
	
	private SimpleScriptStorage scriptStorage;
	private ITreeNode parentNode;

	public C4ScriptExtern(C4GroupItem script, ITreeNode parentNode) {
		this.parentNode = parentNode;
		if (parentNode != null)
			parentNode.addChild(this);
		setName(script.getName());
		scriptStorage = new SimpleScriptStorage((C4GroupEntry) script);
	}
	
	@Override
	public ClonkIndex getIndex() {
		return ClonkCore.getDefault().getExternIndex();
	}

	@Override
	public Object getScriptFile() {
		return scriptStorage;
	}
	
	@Override
	public ITreeNode getParentNode() {
		return parentNode;
	}

}
