package net.arctics.clonk.ui.editors.ini;

import net.arctics.clonk.parser.inireader.IntegerArray;
import net.arctics.clonk.util.IHasContext;
import net.arctics.clonk.util.IHasKeyAndValue;

public class IntegerArrayItem implements IHasKeyAndValue<String, String>, IHasContext {
	
	private IntegerArray array;
	private int index;
	private Object context;
	
	public IntegerArrayItem(IntegerArray array, int index, Object context) {
		super();
		this.array = array;
		this.index = index;
		this.context = context;
	}
	public IntegerArray getArray() {
		return array;
	}
	public int getIndex() {
		return index;
	}
	public void setValue(int value) {
		array.set(index, value);
	}
	public String getKey() {
		return "["+String.valueOf(index)+"]";
	}
	public void setValue(String value) {
		setValue(Integer.valueOf(value));
	}
	public String getValue() {
		return String.valueOf(array.get(index));
	}
	public Object context() {
		return context;
	}
}