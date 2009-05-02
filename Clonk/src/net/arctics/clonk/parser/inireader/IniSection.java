/**
 * 
 */
package net.arctics.clonk.parser.inireader;

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.IPath;

import net.arctics.clonk.parser.C4Field;
import net.arctics.clonk.parser.SourceLocation;
import net.arctics.clonk.parser.inireader.IniData.IniSectionData;
import net.arctics.clonk.util.IHasChildren;
import net.arctics.clonk.util.IHasKeyAndValue;
import net.arctics.clonk.util.ITreeNode;

public class IniSection extends C4Field implements IHasKeyAndValue<String, String>, IHasChildren, ITreeNode {
	
	private static final long serialVersionUID = 1L;
	
	private Map<String, IniEntry> entries;
	private IniSectionData sectionData;
	
	public IniSectionData getSectionData() {
		return sectionData;
	}

	public void setSectionData(IniSectionData sectionData) {
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
		return "";
	}

	public Object[] getChildren() {
		return getEntries().values().toArray();
	}

	public boolean hasChildren() {
		return !entries.isEmpty();
	}

	public void setValue(String value) {
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
	
}