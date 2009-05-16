package net.arctics.clonk.parser.inireader;

/**
 * Entry class for those Definition[1-9] entries
 * @author madeen
 *
 */
public class DefinitionPack implements IEntryCreateable {

	private String value;
	
	@Override
	public String toString() {
		return value;
	}
	
	public void setInput(String value) throws IniParserException {
		this.value = value;
	}

}
