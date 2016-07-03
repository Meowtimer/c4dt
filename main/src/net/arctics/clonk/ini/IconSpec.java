package net.arctics.clonk.ini;

import net.arctics.clonk.Core;
import net.arctics.clonk.ini.IniData.IniEntryDefinition;

public class IconSpec extends IniEntryValue implements IConvertibleToPrimitive {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private String definition;
	private int index;

	@Override
	public Object convertToPrimitive() {
		return definition;
	}

	@Override
	public void setInput(final String value, final IniEntryDefinition entryData, final IniUnit context) throws IniParserException {
		final String[] split = value.split(":");
		definition = split[0];
		if (split.length > 1) try {
			index = Integer.parseInt(split[1]);
		} catch (final NumberFormatException e) {
			index = 0;
		}
	}

	@Override
	public String toString() {
		return String.format("%s:%d", definition, index);
	}

}
