package net.arctics.clonk.parser.inireader;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.inireader.IniData.IniDataSection;
import net.arctics.clonk.util.IHasChildren;
import net.arctics.clonk.util.IHasKeyAndValue;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.ReadOnlyIterator;

public class IniSection extends C4Declaration implements IHasKeyAndValue<String, String>, IHasChildren, Iterable<IniItem>, IniItem {
	
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private Map<String, IniItem> items;
	private IniDataSection sectionData;
	private int indentation;
	
	public IniDataSection getSectionData() {
		return sectionData;
	}

	public void setSectionData(IniDataSection sectionData) {
		this.sectionData = sectionData;
	}

	protected IniSection(SourceLocation location, String name) {
		this.location = location;
		this.name = name;
	}

	public int getStartPos() {
		return getLocation().getStart();
	}

	public String getName() {
		return name;
	}

	public Map<String, IniItem> getSubItems() {
		return items;
	}

	public void setSubItems(Map<String, IniItem> entries) {
		this.items = entries;
	}

	public IniItem getSubItem(String key) {
		return items.get(key);
	}

	public String getKey() {
		return getName();
	}

	public String getValue() {
		return ""; //$NON-NLS-1$
	}

	public Object[] getChildren() {
		return getSubItems().values().toArray();
	}

	public boolean hasChildren() {
		return !items.isEmpty();
	}

	@Override
	public void setValue(String value, Object context) {
		// FIXME?
	}

	public void addChild(ITreeNode node) {		
	}

	public Collection<? extends ITreeNode> getChildCollection() {
		return items.values();
	}

	public String getNodeName() {
		return getName();
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
	
	@Override
	public String toString() {
		return ((IniUnit)getParentDeclaration()).sectionToString(this);
	}
	
	@Override
	public Object[] getSubDeclarationsForOutline() {
		return this.getChildren();
	}
	
	@Override
	public boolean hasSubDeclarationsInOutline() {
		return hasChildren();
	}

	public Iterator<IniItem> iterator() {
		return new ReadOnlyIterator<IniItem>(this.items.values().iterator());
	}
	
	public void putEntry(IniEntry entry) {
		items.put(entry.getName(), entry);
		entry.setParentDeclaration(this);
	}

	@Override
	public void writeTextRepresentation(Writer writer, int indentation) throws IOException {
		writer.append('[');
		writer.append(getName());
		writer.append(']');
		writer.append('\n');
		
		for (IniItem entry : getSubItems().values()) {
			entry.writeTextRepresentation(writer, indentation+1);
			writer.append('\n');
		}
	}

	@Override
	public void validate() {
		for (IniItem e : this) {
			e.validate();
		}
	}
	
	public int getIndentation() {
		return indentation;
	}

	public void setIndentation(int indentation) {
		this.indentation = indentation;
	}
}