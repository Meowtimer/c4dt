package net.arctics.clonk.parser.inireader;

public class NamedReference implements IEntryCreateable {

	private String value;
	
	public void setInput(String value) throws IniParserException {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value;
	}

}
