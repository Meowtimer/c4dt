package net.arctics.clonk.parser.inireader;


import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;

import org.eclipse.core.resources.IMarker;

public class SignedInteger implements IIniEntry {

	private int x;
	
	public SignedInteger(int i) {
		x = i;
	}
	
	public SignedInteger() {
	}
	
	public String getStringRepresentation() {
		return Integer.toString(x);
	}

	public void setInput(String input, IniDataEntry entryData) throws IniParserException {
		try {
			input = input != null ? input.trim() : "";
			if (input.equals(""))
				x = 0;
			else
				x = Integer.parseInt(input.trim());
		}
		catch(NumberFormatException e) {
			IniParserException exp = new IniParserException(IMarker.SEVERITY_ERROR, "Expected an integer instead of '" + input + "'");
			exp.setInnerException(e);
			throw exp;
		}
	}
	
	@Override
	public String toString() {
		return String.valueOf(x);
	}

}
