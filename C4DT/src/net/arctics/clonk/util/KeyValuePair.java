package net.arctics.clonk.util;

import java.util.Collection;

import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.EntrySubItem;

public class KeyValuePair<First, Second> extends Pair<First, Second> implements IHasKeyAndValue<First, Second>, IHasChildrenWithContext, ITreeNode {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public KeyValuePair(First first, Second second) {
		super(first, second);
	}

	@Override
	public First getKey() {
		return first();
	}

	@Override
	public Second getValue() {
		return  second();
	}

	@Override
	public void setValue(Second value, Object context) {
		setSecond(value);
	}
	
	@Override
	public String toString() {
		return getKey().toString()+"="+getValue().toString(); //$NON-NLS-1$
	}

	@Override
	public Object getChildValue(int index) {
		return index == 0 ? getKey() : getValue(); 
	}

	@Override
	public IHasContext[] getChildren(Object context) {
		IHasContext[] result = new IHasContext[2];
		for (int i = 0; i < 2; i++)
			result[i] = new EntrySubItem<KeyValuePair<First, Second>>(this, context, i);
		return result;
	}

	@Override
	public boolean hasChildren() {
		return true;
	}

	@Override
	public void setChildValue(int index, Object value) {
		
	}

	@Override
	public void addChild(ITreeNode node) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<? extends ITreeNode> getChildCollection() {
		return null;
	}

	@Override
	public String nodeName() {
		return (String) getKey();
	}

	@Override
	public ITreeNode getParentNode() {
		return null;
	}

	@Override
	public IPath getPath() {
		return ITreeNode.Default.getPath(this);
	}

	@Override
	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}

}
