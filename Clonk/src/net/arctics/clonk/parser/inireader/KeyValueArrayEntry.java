package net.arctics.clonk.parser.inireader;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import net.arctics.clonk.util.IHasChildrenWithContext;
import net.arctics.clonk.util.IHasContext;
import net.arctics.clonk.util.KeyValuePair;

public abstract class KeyValueArrayEntry<KeyType, ValueType> implements IEntryCreateable, IHasChildrenWithContext {
	private final List<KeyValuePair<KeyType, ValueType>> components = new ArrayList<KeyValuePair<KeyType, ValueType>>();
	
	public KeyValueArrayEntry(String value) throws IniParserException {
		setInput(value);
	}
	
	public KeyValueArrayEntry() {
	}
	
	public void add(KeyType id, ValueType num) {
		components.add(new KeyValuePair<KeyType, ValueType>(id,num));
	}

	public List<KeyValuePair<KeyType, ValueType>> getComponents() {
		return components;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder(components.size() * 7); // MYID=1;
		ListIterator<KeyValuePair<KeyType, ValueType>> it = components.listIterator();
		while (it.hasNext()) {
			KeyValuePair<KeyType, ValueType> pair = it.next();
			builder.append(pair.toString());
			if (it.hasNext()) builder.append(';');
		}
		return builder.toString();
	}
	
	public abstract KeyValuePair<KeyType, ValueType> singleComponentFromString(String s);

	public void setInput(String input) throws IniParserException {
		// CLNK=1;STIN=10;
		components.clear();
		String[] parts = input.split(";");
		for(String part : parts) {
			if (part.contains("=")) {
				KeyValuePair<KeyType, ValueType> kv = singleComponentFromString(part);
				if (kv != null)
					components.add(kv);
			}
		}
	}

	public IHasContext[] getChildren(Object context) {
		IHasContext[] result = new IHasContext[components.size()];
		for (int i = 0; i < components.size(); i++) {
			result[i] = new EntrySubItem<KeyValueArrayEntry<KeyType, ValueType>>(this, context, i);
		}
		return result;
	}

	public boolean hasChildren() {
		return components.size() > 0;
	}

	public Object getChildValue(int index) {
		return components.get(index);
	}

	@SuppressWarnings("unchecked")
	public void setChildValue(int index, Object value) {
		KeyValuePair<KeyType, ValueType> kv;
		if (value instanceof KeyValuePair)
			kv = (KeyValuePair<KeyType, ValueType>) value;
		else if (value instanceof String)
			kv = singleComponentFromString((String) value);
		else
			kv = null;
		if (kv != null)
			components.set(index, kv);
	}
	
}
