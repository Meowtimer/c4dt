package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;


public interface IIniEntryValue {
	public void setInput(String value, IniDataEntry entryData) throws IniParserException;
}
