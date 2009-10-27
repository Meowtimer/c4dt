package net.arctics.clonk.parser.inireader;

import java.io.InvalidClassException;

import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;

public interface IEntryFactory {
	public Object create(Class<?> type, String value, IniDataEntry entryData) throws InvalidClassException, IniParserException;
}
