package net.arctics.clonk.parser.inireader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.util.IHasChildrenWithContext;
import net.arctics.clonk.util.IHasContext;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.KeyValuePair;

public abstract class KeyValueArrayEntry<KeyType, ValueType> implements IIniEntry, IHasChildrenWithContext, ITreeNode {
	private final List<KeyValuePair<KeyType, ValueType>> components = new ArrayList<KeyValuePair<KeyType, ValueType>>();
	
	public KeyValueArrayEntry(String value, IniDataEntry entryData) throws IniParserException {
		setInput(value, entryData);
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
		Iterator<KeyValuePair<KeyType, ValueType>> it = components.iterator();
		while (it.hasNext()) {
			KeyValuePair<KeyType, ValueType> pair = it.next();
			builder.append(pair.toString());
			if (it.hasNext()) builder.append(';');
		}
		return builder.toString();
	}
	
	public abstract KeyValuePair<KeyType, ValueType> singleComponentFromString(String s);

	public void setInput(String input, IniDataEntry entryData) throws IniParserException {
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
	
	public Collection<? extends ITreeNode> getChildCollection() {
		return components;
	}
	
	public String getNodeName() {
		return null;
	}
	
	public void addChild(ITreeNode node) {
		
	}
	
	public ITreeNode getParentNode() {
		return null;
	}
	
	public IPath getPath() {
		return ITreeNode.Default.getPath(this);
	}
	
	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}
	
}
