package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;

public class NamedReference implements IIniEntryValue {

	private String value;
	
	@Override
	public void setInput(String value, IniDataEntry entryData, IniUnit context) throws IniParserException {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value;
	}

}
