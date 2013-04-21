package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.parser.inireader.IniData.IniEntryDefinition;

/**
 * Specialization to have fancy checkboxes for it in the ini editor
 */
public class Boolean extends UnsignedInteger {
	@Override
	public Object convertToPrimitive() {
		return this.getNumber() != 0;
	}
	@Override
	public void setInput(String input, IniEntryDefinition entryData, IniUnit context) throws IniParserException {
		if (input.equals(Keywords.True))
			setNumber(1);
		else if (input.equals(Keywords.False))
			setNumber(0);
		else
			super.setInput(input, entryData, context);
	}
	public Boolean() {}
	public Boolean(boolean value) throws IniParserException {
		setNumber(value ? 1 : 0);
	}
}
