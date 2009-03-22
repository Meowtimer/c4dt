/**
 * 
 */
package net.arctics.clonk.parser.inireader;

import java.util.Map;

import net.arctics.clonk.util.IHasChildren;
import net.arctics.clonk.util.IHasKeyAndValue;

public class IniSection implements IHasKeyAndValue<String, String>, IHasChildren {
	private int startPos;
	private String name;
	private Map<String, IniEntry> entries;
	
	protected IniSection(int pos, String name) {
		startPos = pos;
		this.name = name;
	}

	public int getStartPos() {
		return startPos;
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
	
}