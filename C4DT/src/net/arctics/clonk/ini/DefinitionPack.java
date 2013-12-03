package net.arctics.clonk.ini;

import net.arctics.clonk.Core;
import net.arctics.clonk.ini.IniData.IniEntryDefinition;

/**
 * Entry class for those Definition[1-9] entries
 * @author madeen
 *
 */
public class DefinitionPack extends IniEntryValue {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private String value;
	@Override
	public String toString() { return value; }
	@Override
	public void setInput(final String value, final IniEntryDefinition entryData, final IniUnit context) throws IniParserException {
		this.value = value;
	}
}
