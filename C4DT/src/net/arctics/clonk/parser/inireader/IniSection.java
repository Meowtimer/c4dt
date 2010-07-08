package net.arctics.clonk.parser.inireader;

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

public class IniSection extends C4Declaration implements IHasKeyAndValue<String, String>, IHasChildren, ITreeNode, Iterable<IniEntry> {
	
	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private Map<String, IniEntry> entries;
	private IniDataSection sectionData;
	
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

	public Map<String, IniEntry> getEntries() {
		return entries;
	}

	public void setEntries(Map<String, IniEntry> entries) {
		this.entries = entries;
	}

	public IniEntry getEntry(String key) {
		return entries.get(key);
	}

	public String getKey() {
		return getName();
	}

	public String getValue() {
		return ""; //$NON-NLS-1$
	}

	public Object[] getChildren() {
		return getEntries().values().toArray();
	}

	public boolean hasChildren() {
		return !entries.isEmpty();
	}

	@Override
	public void setValue(String value, Object context) {
		// FIXME?
	}

	public void addChild(ITreeNode node) {		
	}

	public Collection<? extends ITreeNode> getChildCollection() {
		return entries.values();
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

	public Iterator<IniEntry> iterator() {
		return new ReadOnlyIterator<IniEntry>(this.entries.values().iterator());
	}
	
	public void putEntry(IniEntry entry) {
		entries.put(entry.getName(), entry);
		entry.setParentDeclaration(this);
	}
	
}