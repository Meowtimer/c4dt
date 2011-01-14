package net.arctics.clonk.parser.inireader;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.inireader.IniData.IniDataSection;
import net.arctics.clonk.util.IHasChildren;
import net.arctics.clonk.util.IHasKeyAndValue;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.ReadOnlyIterator;

public class IniSection extends Declaration implements IHasKeyAndValue<String, String>, IHasChildren, Iterable<IniItem>, IniItem {
	
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private Map<String, IniItem> itemMap;
	private List<IniItem> itemList;
	private IniDataSection sectionData;
	private int indentation;
	private int sectionEnd;
	
	public int getSectionEnd() {
		return sectionEnd;
	}
	
	public void setSectionEnd(int sectionEnd) {
		this.sectionEnd = sectionEnd;
	}
	
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

	public Map<String, IniItem> getSubItemMap() {
		return itemMap;
	}

	public void setSubItems(Map<String, IniItem> map, List<IniItem> list) {
		this.itemMap = map;
		this.itemList = list;
	}

	public IniItem getSubItem(String key) {
		return itemMap.get(key);
	}
	
	public List<IniItem> getSubItemList() {
		return itemList;
	}

	public String getKey() {
		return getName();
	}

	public String getValue() {
		return ""; //$NON-NLS-1$
	}

	public Object[] getChildren() {
		return getSubItemList().toArray(new Object[getSubItemList().size()]);
	}

	public boolean hasChildren() {
		return !itemMap.isEmpty();
	}

	@Override
	public void setValue(String value, Object context) {
		// FIXME?
	}

	public void addChild(ITreeNode node) {		
	}

	public Collection<? extends IniItem> getChildCollection() {
		return itemList;
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
		IniUnit unit = getIniUnit();
		return unit != null ? unit.sectionToString(this) : getName();
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
		return new ReadOnlyIterator<IniItem>(this.itemMap.values().iterator());
	}
	
	public void putEntry(IniEntry entry) {
		itemMap.put(entry.getName(), entry);
		itemList.add(entry);
		entry.setParentDeclaration(this);
	}

	@Override
	public void writeTextRepresentation(Writer writer, int indentation) throws IOException {
		writer.append('[');
		writer.append(getName());
		writer.append(']');
		writer.append('\n');
		
		for (IniItem entry : getSubItemMap().values()) {
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
	
	@Override
	public int sortCategory() {
		return 1;
	}
	
	public Iterable<IniSection> getSections() {
		// unable to make this work generically ;c
		List<IniSection> sections = new LinkedList<IniSection>();
		for (ITreeNode node : getChildCollection()) {
			if (node instanceof IniSection) {
				sections.add((IniSection)node);
			}
		}
		return sections;
	}
	
	public IniSection getParentSection() {
		return getParentDeclaration() instanceof IniSection ? (IniSection)getParentDeclaration() : null;
	}
	
	public IniUnit getIniUnit() {
		return getParentDeclarationOfType(IniUnit.class);
	}

}