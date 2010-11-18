package net.arctics.clonk.parser.inireader;



import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;

public class CategoriesArray extends IniEntryValueBase {

	private CategoriesValue value;
	
	public CategoriesArray() {
	}
	
	public String toString() {
		return value != null ? value.toString() : "<Empty>";
	}

	public void setInput(String input, IniDataEntry entryData, IniUnit context) throws IniParserException {
		value = new CategoriesValue(input, context.getEngine(), entryData.getConstantsPrefix());
	}

}
