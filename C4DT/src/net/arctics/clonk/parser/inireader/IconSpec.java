package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;

public class IconSpec extends IniEntryValueBase implements IConvertibleToPrimitive {

	private String Definition;
	private int Index;
	
	@Override
	public Object convertToPrimitive() {
		return Definition;
	}

	@Override
	public void setInput(String value, IniDataEntry entryData, IniUnit context) throws IniParserException {
		String[] split = value.split(":");
		Definition = split[0];
		if (split.length > 1) try {
			Index = Integer.parseInt(split[1]);
		} catch (NumberFormatException e) {
			Index = 0;
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s:%d", Definition, Index);
	}

}
