package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.inireader.IniData.IniEntryDefinition;

public interface IEntryFactory {
	public Object create(Class<?> type, String value, IniEntryDefinition entryData, IniUnit context) throws IniParserException;
}
