package net.arctics.clonk.ini;

import net.arctics.clonk.ini.IniData.IniEntryDefinition;


public interface IIniEntryValue {
	void setInput(String value, IniEntryDefinition entryData, IniUnit context) throws IniParserException;
	Object evaluate(Object context);
}
