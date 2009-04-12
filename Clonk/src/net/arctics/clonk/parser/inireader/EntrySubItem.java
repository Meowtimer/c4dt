package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.util.IHasChildrenWithContext;
import net.arctics.clonk.util.IHasContext;
import net.arctics.clonk.util.IHasKeyAndValue;

/**
 * Sub item of an ini entry.
 *
 * @param <EntryType> Entry class
 */
public class EntrySubItem<EntryType extends IHasChildrenWithContext> implements IHasContext,
		IHasKeyAndValue<String, String>, IHasChildrenWithContext {
	
	public EntrySubItem(EntryType entry, Object context, int index) {
		super();
		this.entry = entry;
		this.context = context;
		this.index = index;
	}

	private EntryType entry;
	private Object context;
	private int index;

	public Object context() {
		return context;
	}

	public String getKey() {
		return "["+String.valueOf(index)+"]";
	}

	public String getValue() {
		return entry.getChildValue(index).toString();
	}

	public void setValue(String value) {
		entry.setChildValue(index, value);
	}

	public Object getChildValue(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	public IHasContext[] getChildren(Object context) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasChildren() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setChildValue(int index, Object value) {
		// TODO Auto-generated method stub
		
	}

}
