package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;


public interface IIniEntryValue {
	void setInput(String value, IniDataEntry entryData, IniUnit context) throws IniParserException;
	Object evaluate(Object context);
}
