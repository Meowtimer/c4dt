package net.arctics.clonk.parser.inireader;

import org.eclipse.core.resources.IMarker;

import net.arctics.clonk.parser.inireader.IniData.IniEntryDefinition;

public class Enum extends UnsignedInteger {
	@Override
	protected void setNumberFromStringValue(String input, IniEntryDefinition entryData, IniUnit context) throws IniParserException {
		Integer i = entryData.enumValues() != null ? entryData.enumValues().get(input) : null;
		if (i == null)
			throw new IniParserException(IMarker.SEVERITY_ERROR, String.format(Messages.Enum_UnknownValue, input));
		setNumber(i);
	}
}
