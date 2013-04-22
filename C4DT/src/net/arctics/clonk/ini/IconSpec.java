package net.arctics.clonk.ini;

import net.arctics.clonk.ini.IniData.IniEntryDefinition;

public class IconSpec extends IniEntryValueBase implements IConvertibleToPrimitive {

	private String definition;
	private int index;
	
	@Override
	public Object convertToPrimitive() {
		return definition;
	}

	@Override
	public void setInput(String value, IniEntryDefinition entryData, IniUnit context) throws IniParserException {
		String[] split = value.split(":");
		definition = split[0];
		if (split.length > 1) try {
			index = Integer.parseInt(split[1]);
		} catch (NumberFormatException e) {
			index = 0;
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s:%d", definition, index);
	}

}
