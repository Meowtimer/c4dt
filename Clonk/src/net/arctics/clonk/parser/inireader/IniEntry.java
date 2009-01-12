/**
 * 
 */
package net.arctics.clonk.parser.inireader;

public class IniEntry {
	private int startPos;
	private String key;
	private String value;
	
	public IniEntry(int pos, String k, String v) {
		startPos = pos;
		key = k;
		value = v;
	}

	public int getStartPos() {
		return startPos;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}
}