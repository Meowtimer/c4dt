/**
 * 
 */
package net.arctics.clonk.parser.inireader;

import java.util.Map;


public class IniSection {
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
	
}