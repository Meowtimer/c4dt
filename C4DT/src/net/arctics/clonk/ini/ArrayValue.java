package net.arctics.clonk.ini;

import static net.arctics.clonk.util.Utilities.as;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ini.IniData.IniEntryDefinition;
import net.arctics.clonk.util.IHasChildrenWithContext;
import net.arctics.clonk.util.IHasContext;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.KeyValuePair;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;

public abstract class ArrayValue<KeyType, ValueType> extends IniEntryValue implements IHasChildrenWithContext, ITreeNode {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final List<KeyValuePair<KeyType, ValueType>> components = new ArrayList<KeyValuePair<KeyType, ValueType>>();
	public ArrayValue(String value, IniEntryDefinition entryData, IniUnit context) throws IniParserException { setInput(value, entryData, context); }
	public ArrayValue() {}
	public void add(KeyType id, ValueType num) { components.add(new KeyValuePair<KeyType, ValueType>(id,num)); }
	public void add(KeyValuePair<KeyType, ValueType> pair) { components.add(pair); }
	public KeyValuePair<KeyType, ValueType> find(KeyType key) {
		for (final KeyValuePair<KeyType, ValueType> kv : components)
			if (kv.key().equals(key))
				return kv;
		return null;
	}
	public List<KeyValuePair<KeyType, ValueType>> components() { return components; }
	public abstract KeyValuePair<KeyType, ValueType> singleComponentFromString(int offset, String s);
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
		int off = 0;
		for (final String part : parts) {
			boolean valid = false;
			if (part.contains("=")) { //$NON-NLS-1$
				KeyValuePair<KeyType, ValueType> kv;
				try {
					kv = singleComponentFromString(off, part);
				} catch (final IllegalArgumentException e) {
					kv = null;
				}
				if (kv != null) {
					components.add(kv);
					valid = true;
				}
			}
			if (!valid) {
				if (invalidParts == null)
					invalidParts = new ArrayList<>(3);
					invalidParts.add(part);
			}
			off += part.length()+1;
		}
		assignParentToSubElements();
		if (invalidParts != null)
			throw new IniParserException(IMarker.SEVERITY_ERROR,
				String.format(Messages.InvalidParts, StringUtil.blockString("", "", ",  ", invalidParts))); //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Override
	public IHasContext[] children(Object context) {
		final IHasContext[] result = new IHasContext[components.size()];
		for (int i = 0; i < components.size(); i++)
			result[i] = new EntrySubItem<ArrayValue<KeyType, ValueType>>(this, context, i);
		return result;
	}
	@Override
	public ASTNode[] subElements() {
		final ASTNode[] nodes = new ASTNode[components.size()*2];
		for (int i = 0, j = 0; j < components.size(); i += 2, j++) {
			nodes[i]   = as(components.get(j).key(), ASTNode.class);
			nodes[i+1] = as(components.get(j).value(), ASTNode.class);
		}
		return nodes;
	}
}
