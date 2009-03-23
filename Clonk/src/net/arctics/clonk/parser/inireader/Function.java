package net.arctics.clonk.parser.inireader;

public class Function implements IEntryCreateable {

	private String value;
	
	public void setInput(String value) throws IniParserException {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value;
	}

}
