package net.arctics.clonk.parser.inireader;

import java.io.InvalidClassException;

public interface IEntryFactory {
	public Object create(Class<?> type, String value) throws InvalidClassException, IniParserException;
}
