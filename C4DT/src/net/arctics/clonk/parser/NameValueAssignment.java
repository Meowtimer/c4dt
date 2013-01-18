package net.arctics.clonk.parser;

import java.util.Collection;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.IIndexEntity;
import net.arctics.clonk.util.IHasKeyAndValue;
import net.arctics.clonk.util.INode;
import net.arctics.clonk.util.ITreeNode;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Declaration of some kind consisting basically of a {@link #name()} being assigned a {@link #stringValue()}.
 * @author madeen
 *
 */
public class NameValueAssignment extends Declaration implements IHasKeyAndValue<String, String>, ITreeNode {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private String value;
	
	public NameValueAssignment(int start, int end, String k, String v) {
		super(start, end);
		this.name = k;
		value = v;
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
		return ITreeNode.Default.path(this);
	}

	@Override
	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}
	
	@Override
	public IRegion regionToSelect() {
		return new Region(start()+getLength()-value.length(), value.length());
	}
	
	@Override
	public String infoText(IIndexEntity context) {
	    return key() + "=" + stringValue(); //$NON-NLS-1$
	}

}
