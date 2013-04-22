package net.arctics.clonk.ini;

import net.arctics.clonk.ini.IniData.IniEntryDefinition;

public interface IEntryFactory {
	public Object create(Class<?> type, String value, IniEntryDefinition entryData, IniUnit context) throws IniParserException;
}
