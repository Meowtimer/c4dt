package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;

public class NamedReference implements IIniEntry {

	private String value;
	
	public void setInput(String value, IniDataEntry entryData) throws IniParserException {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value;
	}

}
