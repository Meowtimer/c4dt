package net.arctics.clonk.parser.inireader;



import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;

public class CategoriesArray extends IniEntryValueBase {

	private CategoriesValue value;
	
	public CategoriesArray() {
	}
	
	@Override
	public String toString() {
		return value != null ? value.toString() : "<Empty>";
	}

	@Override
	public void setInput(String input, IniDataEntry entryData, IniUnit context) throws IniParserException {
		value = new CategoriesValue(input, context.engine(), entryData.constantsPrefix());
	}

}
