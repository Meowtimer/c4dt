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

	@Override
	public String key() {
		return name;
	}

	@Override
	public String stringValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return key() + "=" + stringValue(); //$NON-NLS-1$
	}

	@Override
	public void setStringValue(String value, Object context) {
		this.value = value;
	}

	@Override
	public int getLength() {
		return getEndPos() - getStartPos();
	}

	@Override
	public int getOffset() {
		return getStartPos();
	}

	@Override
	public void addChild(ITreeNode node) {
	}

	@Override
	public Collection<? extends INode> childCollection() {
		return null;
	}

	@Override
	public String nodeName() {
		return key();
	}

	@Override
	public ITreeNode parentNode() {
		if (parentDeclaration instanceof ITreeNode)
			return (ITreeNode) parentDeclaration;
		return null;
	}

	@Override
	public IPath path() {
		return ITreeNode.Default.getPath(this);
	}

	@Override
	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}
	
	@Override
	public IRegion regionToSelect() {
		SourceLocation loc = location();
		return new Region(loc.getOffset()+loc.getLength()-value.length(), value.length());
	}
	
	@Override
	public String infoText() {
	    return key() + "=" + stringValue(); //$NON-NLS-1$
	}

}
