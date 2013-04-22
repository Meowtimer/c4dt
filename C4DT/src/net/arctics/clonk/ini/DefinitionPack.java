package net.arctics.clonk.ini;

import net.arctics.clonk.ini.IniData.IniEntryDefinition;

/**
 * Entry class for those Definition[1-9] entries
 * @author madeen
 *
 */
public class DefinitionPack extends IniEntryValueBase {
	private String value;
	@Override
	public String toString() { return value; }
	@Override
	public void setInput(String value, IniEntryDefinition entryData, IniUnit context) throws IniParserException {
		this.value = value;
	}
}
