package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.inireader.IniData.IniEntryDefinition;


public interface IIniEntryValue {
	void setInput(String value, IniEntryDefinition entryData, IniUnit context) throws IniParserException;
	Object evaluate(Object context);
}
