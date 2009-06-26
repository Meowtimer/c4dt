package net.arctics.clonk.index;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.SimpleScriptStorage;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.resource.c4group.C4GroupItem;
import net.arctics.clonk.util.ITreeNode;

public class C4ObjectExtern extends C4Object implements ITreeNode {

	private static final long serialVersionUID = -4964785375712432236L;
	
	private SimpleScriptStorage script;
	private ITreeNode parentNode;
	private String nodeName;
	private List<ITreeNode> childNodes;
	
	public C4ObjectExtern(C4ID id, String name, C4GroupItem script, ITreeNode parentNode) {
		super(id, name);
		this.script = script != null ? new SimpleScriptStorage((C4GroupEntry)script) : null;
		this.parentNode = parentNode;
		if (parentNode != null)
			parentNode.addChild(this);
		if (script != null && script.getParentGroup() != null)
			this.nodeName = script.getParentGroup().getName();
		else
			this.nodeName = name;
	}

	@Override
	public Object getScriptFile() {
		return script;
	}

	@Override
	public ClonkIndex getIndex() {
		if (this == ClonkCore.getDefault().getEngineObject())
			return null;
		return ClonkCore.getDefault().externIndex;
	}

	// return name of folder instead of name of object (so constructed paths don't look wierd)
	public String getNodeName() {
		if (nodeName == null)
			nodeName = getName();
		return nodeName;
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
		if (childNodes == null)
			childNodes = new ArrayList<ITreeNode>();
		return childNodes;
	}

	public void addChild(ITreeNode node) {
		getChildCollection().add(node);
	}
	
	@Override
	public boolean isEditable() {
		return false;
	}

}
