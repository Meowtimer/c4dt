package net.arctics.clonk.parser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.util.ITreeNode;

public class C4ObjectExternGroup implements ITreeNode, Serializable {

	private static final long serialVersionUID = 1L;
	
	private String nodeName;
	private ITreeNode parentNode;
	private List<ITreeNode> childNodes;
	
	public String nodeName() {
		return nodeName;
	}

	public ITreeNode parentNode() {
		return parentNode;
	}

	public C4ObjectExternGroup(String nodeName, ITreeNode parentNode) {
		super();
		this.nodeName = nodeName;
		this.parentNode = parentNode;
		if (parentNode != null)
			this.parentNode.addChild(this);
	}

	public IPath getPath() {
		return ITreeNode.Default.getPath(this);
	}
	
	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

	public List<? extends ITreeNode> getChildren() {
		if (childNodes == null)
			childNodes = new ArrayList<ITreeNode>();
		return childNodes;
	}

	public void addChild(ITreeNode node) {
		childNodes.add(node);
	}
	
	@Override
	public String toString() {
		return nodeName;
	}

}
