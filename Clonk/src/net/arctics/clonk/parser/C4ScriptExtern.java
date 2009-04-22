package net.arctics.clonk.parser;

import java.util.List;

import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.util.ITreeNode;

public class C4ScriptExtern extends C4ScriptBase implements ITreeNode {
	
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
		return ClonkCore.getDefault().EXTERN_INDEX;
	}

	@Override
	public Object getScriptFile() {
		return scriptStorage;
	}

	public String getNodeName() {
		return getName();
	}

	public ITreeNode getParentNode() {
		return parentNode;
	}

	public IPath getPath() {
		return ITreeNode.Default.getPath(this);
	}
	
	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

	public List<ITreeNode> getChildCollection() {
		return null;
	}

	public void addChild(ITreeNode node) {
		// nope
	}

}
