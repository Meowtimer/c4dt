package net.arctics.clonk.ini;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.arctics.clonk.ini.IniData.IniEntryDefinition;
import net.arctics.clonk.util.IHasChildrenWithContext;
import net.arctics.clonk.util.IHasContext;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.KeyValuePair;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;

public abstract class KeyValueArrayEntry<KeyType, ValueType> extends IniEntryValueBase implements IHasChildrenWithContext, ITreeNode {
	private final List<KeyValuePair<KeyType, ValueType>> components = new ArrayList<KeyValuePair<KeyType, ValueType>>();
	public KeyValueArrayEntry(String value, IniEntryDefinition entryData, IniUnit context) throws IniParserException { setInput(value, entryData, context); }
	public KeyValueArrayEntry() {}
	public void add(KeyType id, ValueType num) { components.add(new KeyValuePair<KeyType, ValueType>(id,num)); }
	public void add(KeyValuePair<KeyType, ValueType> pair) { components.add(pair); }
	public KeyValuePair<KeyType, ValueType> find(KeyType key) {
		for (final KeyValuePair<KeyType, ValueType> kv : components)
			if (kv.key().equals(key))
				return kv;
		return null;
	}
	public List<KeyValuePair<KeyType, ValueType>> components() { return components; }
	public abstract KeyValuePair<KeyType, ValueType> singleComponentFromString(String s);
	@Override
	public boolean hasChildren() { return components.size() > 0; }
	@Override
	public Object valueOfChildAt(int index) { return components.get(index); }
	@Override
	public String nodeName() { return null; }
	@Override
	public void addChild(ITreeNode node) {}
	@Override
	public ITreeNode parentNode() { return null; }
	@Override
	public IPath path() { return ITreeNode.Default.path(this); }
	@Override
	public boolean subNodeOf(ITreeNode node) { return ITreeNode.Default.subNodeOf(this, node); }
	@Override
	public boolean isEmpty() { return components == null || components.size() == 0; }
	@Override
	public List<KeyValuePair<KeyType, ValueType>> childCollection() { return components; }
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(components.size() * 7); // MYID=1;
		final Iterator<KeyValuePair<KeyType, ValueType>> it = components.iterator();
		while (it.hasNext()) {
			final KeyValuePair<KeyType, ValueType> pair = it.next();
			builder.append(pair.toString());
			if (it.hasNext()) builder.append(';');
		}
		return builder.toString();
	}
	@Override
	public void setInput(String input, IniEntryDefinition entryData, IniUnit context) throws IniParserException {
		// CLNK=1;STIN=10;
		components.clear();
		final String[] parts = input.split(";|,"); //$NON-NLS-1$
		List<String> invalidParts = null;
		for(final String part : parts)
			if (part.contains("=")) { //$NON-NLS-1$
				KeyValuePair<KeyType, ValueType> kv;
				try {
					kv = singleComponentFromString(part);
				} catch (final IllegalArgumentException e) {
					kv = null;
				}
				if (kv != null)
					components.add(kv);
				else {
					if (invalidParts == null)
						invalidParts = new ArrayList<>(3);
					invalidParts.add(part);
				}
			}
		if (invalidParts != null)
			throw new IniParserException(IMarker.SEVERITY_ERROR,
				String.format(Messages.InvalidParts, StringUtil.blockString("", "", ",  ", invalidParts))); //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Override
	public IHasContext[] children(Object context) {
		final IHasContext[] result = new IHasContext[components.size()];
		for (int i = 0; i < components.size(); i++)
			result[i] = new EntrySubItem<KeyValueArrayEntry<KeyType, ValueType>>(this, context, i);
		return result;
	}
	@Override
	@SuppressWarnings({ "unchecked" })
	public void setValueOfChildAt(int index, Object value) {
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
