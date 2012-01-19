package net.arctics.clonk.parser;

import java.util.Collection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.util.IHasKeyAndValue;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;

public class NameValueAssignment extends Declaration implements IHasKeyAndValue<String, String>, IRegion, ITreeNode {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
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
		return getKey() + "=" + getValue(); //$NON-NLS-1$
	}

	public void setValue(String value, Object context) {
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

	public Collection<? extends INode> getChildCollection() {
		return null;
	}

	public String nodeName() {
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
	
	@Override
	public IRegion getRegionToSelect() {
		SourceLocation loc = getLocation();
		return new Region(loc.getOffset()+loc.getLength()-value.length(), value.length());
	}
	
	@Override
	public String getInfoText() {
	    return getKey() + "=" + getValue(); //$NON-NLS-1$
	}

}
