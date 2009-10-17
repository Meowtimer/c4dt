package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;

/**
 * Entry class for those Definition[1-9] entries
 * @author madeen
 *
 */
public class DefinitionPack implements IIniEntry {

	private String value;
	
	@Override
	public String toString() {
		return value;
	}
	
	public void setInput(String value, IniDataEntry entryData) throws IniParserException {
		this.value = value;
	}

}
