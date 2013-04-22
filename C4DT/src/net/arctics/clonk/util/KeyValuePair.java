package net.arctics.clonk.util;

import java.util.Collection;

import net.arctics.clonk.Core;
import net.arctics.clonk.ini.EntrySubItem;

import org.eclipse.core.runtime.IPath;

public class KeyValuePair<First, Second> extends Pair<First, Second> implements IHasKeyAndValue<First, Second>, IHasChildrenWithContext, ITreeNode {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public KeyValuePair(First first, Second second) { super(first, second); }
	@Override
	public First key() { return first(); }
	public Second value() { return second(); }
	public void setValue(Second value) { setSecond(value); }
	@Override
	public Second stringValue() { return second(); }
	@Override
	public String toString() { return key().toString()+"="+stringValue().toString(); } //$NON-NLS-1$
	@Override
	public Object valueOfChildAt(int index) { return index == 0 ? key() : stringValue();  }
	@Override
	public IHasContext[] children(Object context) {
		final IHasContext[] result = new IHasContext[2];
		for (int i = 0; i < 2; i++)
			result[i] = new EntrySubItem<KeyValuePair<First, Second>>(this, context, i);
		return result;
	}
	@Override
	public boolean hasChildren() { return true;
	}
	@Override
	public void setValueOfChildAt(int index, Object value) {}
	@Override
	public void addChild(ITreeNode node) {}
	@Override
	public Collection<? extends ITreeNode> childCollection() { return null; }
	@Override
	public String nodeName() { return (String) key(); }
	@Override
	public ITreeNode parentNode() { return null; }
	@Override
	public IPath path() { return ITreeNode.Default.path(this); }
	@Override
	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}
}
