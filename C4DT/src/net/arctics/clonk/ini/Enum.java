package net.arctics.clonk.ini;

import org.eclipse.core.resources.IMarker;

import net.arctics.clonk.Core;
import net.arctics.clonk.ini.IniData.IniEntryDefinition;

public class Enum extends UnsignedInteger {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	@Override
	protected void setNumberFromStringValue(String input, IniEntryDefinition entryData, IniUnit context) throws IniParserException {
		final Integer i = entryData.enumValues() != null ? entryData.enumValues().get(input) : null;
		if (i == null)
			throw new IniParserException(IMarker.SEVERITY_ERROR, String.format(Messages.Enum_UnknownValue, input));
		setNumber(i);
	}
}
