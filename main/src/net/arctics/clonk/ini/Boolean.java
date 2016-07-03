package net.arctics.clonk.ini;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Keywords;
import net.arctics.clonk.ini.IniData.IniEntryDefinition;

/**
 * Specialization to have fancy checkboxes for it in the ini editor
 */
public class Boolean extends UnsignedInteger {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	@Override
	public Object convertToPrimitive() {
		return this.number() != 0;
	}
	public boolean booleanValue() { return number() != 0; }
	@Override
	public void setInput(final String input, final IniEntryDefinition entryData, final IniUnit context) throws IniParserException {
		if (input.equals(Keywords.True))
			setNumber(1);
		else if (input.equals(Keywords.False))
			setNumber(0);
		else
			super.setInput(input, entryData, context);
	}
	public Boolean() {}
	public Boolean(final boolean value) throws IniParserException {
		setNumber(value ? 1 : 0);
	}
}
