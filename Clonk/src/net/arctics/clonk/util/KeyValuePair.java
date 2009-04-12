package net.arctics.clonk.util;

import net.arctics.clonk.parser.inireader.EntrySubItem;

public class KeyValuePair<First, Second> extends Pair<First, Second> implements IHasKeyAndValue<First, Second>, IHasChildrenWithContext {

	public KeyValuePair(First first, Second second) {
		super(first, second);
	}

	public First getKey() {
		return getFirst();
	}

	public Second getValue() {
		return  getSecond();
	}

	public void setValue(Second value) {
		setSecond(value);
	}
	
	@Override
	public String toString() {
		return getKey().toString()+"="+getValue().toString();
	}

	public Object getChildValue(int index) {
		return index == 0 ? getKey() : getValue(); 
	}

	public IHasContext[] getChildren(Object context) {
		IHasContext[] result = new IHasContext[2];
		for (int i = 0; i < 2; i++)
			result[i] = new EntrySubItem<KeyValuePair<First, Second>>(this, context, i);
		return result;
	}

	public boolean hasChildren() {
		return true;
	}

	public void setChildValue(int index, Object value) {
		
	}

}
