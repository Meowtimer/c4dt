package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.inireader.IniData.IniEntryDefinition;

public class NamedReference extends IniEntryValueBase {

	private String value;
	
	@Override
	public void setInput(String value, IniEntryDefinition entryData, IniUnit context) throws IniParserException {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value;
	}

}
