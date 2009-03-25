/**
 * 
 */
package net.arctics.clonk.parser.inireader;

import org.eclipse.jface.text.IRegion;

import net.arctics.clonk.util.IHasKeyAndValue;

public class IniEntry implements IHasKeyAndValue<String, String>, IRegion {
	private int startPos, endPos;
	private String key;
	private String value;
	
	public IniEntry(int pos, int endPos, String k, String v) {
		startPos = pos;
		this.endPos = endPos;
		key = k;
		value = v;
	}

	public int getStartPos() {
		return startPos;
	}
	
	public int getEndPos() {
		return endPos;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return getKey() + "=" + getValue();
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getLength() {
		return endPos - startPos;
	}

	public int getOffset() {
		return startPos;
	}

}