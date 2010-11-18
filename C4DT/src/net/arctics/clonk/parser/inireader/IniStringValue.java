package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.parser.stringtbl.StringTbl;

public class IniStringValue extends IniEntryValueBase {

	private String value; 
	
	@Override
	public Object evaluate(Object context) {
		if (context instanceof C4Declaration) {
			return StringTbl.evaluateEntries((C4Declaration)context, value, 0);
		} else {
			return "";
		} 
	}
	
	@Override
	public void setInput(String value, IniDataEntry entryData, IniUnit context) throws IniParserException {
		this.value = value;
	}

}
