package net.arctics.clonk.parser;

import java.util.Collection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IRegion;

import net.arctics.clonk.util.IHasKeyAndValue;
import net.arctics.clonk.util.ITreeNode;

public class NameValueAssignment extends C4Declaration implements IHasKeyAndValue<String, String>, IRegion, ITreeNode {

	private static final long serialVersionUID = 1L;
	
	private String value;
	
	public NameValueAssignment(int pos, int endPos, String k, String v) {
		this.location = new SourceLocation(pos, endPos);
		this.name = k;
		value = v;
	}

	public int getStartPos() {
		return location.getStart();
	}
	
	public int getEndPos() {
		return location.getEnd();
	}

	public String getKey() {
		return name;
	}

	public String getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return getKey() + "=" + getValue();
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getLength() {
		return getEndPos() - getStartPos();
	}

	public int getOffset() {
		return getStartPos();
	}

	public void addChild(ITreeNode node) {
	}

	public Collection<? extends ITreeNode> getChildCollection() {
		return null;
	}

	public String getNodeName() {
		return getKey();
	}

	public ITreeNode getParentNode() {
		if (parentDeclaration instanceof ITreeNode)
			return (ITreeNode) parentDeclaration;
		return null;
	}

	public IPath getPath() {
		return ITreeNode.Default.getPath(this);
	}

	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

}
