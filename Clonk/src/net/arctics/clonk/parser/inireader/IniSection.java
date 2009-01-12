/**
 * 
 */
package net.arctics.clonk.parser.inireader;


public class IniSection {
	private int startPos;
	private String name;
	private IniEntry[] entries = null;
	
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

	public IniEntry[] getEntries() {
		return entries;
	}

	public void setEntries(IniEntry[] entries) {
		this.entries = entries;
	}
}