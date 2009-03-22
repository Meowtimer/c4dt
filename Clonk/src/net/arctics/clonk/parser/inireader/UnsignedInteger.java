package net.arctics.clonk.parser.inireader;

import java.security.InvalidParameterException;


import org.eclipse.core.resources.IMarker;


public class UnsignedInteger implements IEntryCreateable {
	
	private int number;
	
	public UnsignedInteger(String input) throws IniParserException {
		setInput(input);
	}
	
	public UnsignedInteger(int num) {
		number = num;
	}
	
	public UnsignedInteger() {
	}

	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		if (number >= 0) this.number = number;
		else throw new InvalidParameterException("Only unsigned integers are allowed");
	}

	public String getStringRepresentation() {
		return Integer.toString(this.number);
	}

	public void setInput(String input) throws IniParserException {
		try {
			Integer num = Integer.decode(input);
			number = num.intValue();
			if (num < 0)
				throw new IniParserException(IMarker.SEVERITY_WARNING, "Unsigned value expected");
		}
		catch (NumberFormatException e) {
			IniParserException exp = new IniParserException(IMarker.SEVERITY_ERROR, "Invalid value('" + input + "') given");
			exp.setInnerException(e);
			throw exp;
		}
	}
	
	@Override
	public String toString() {
		return String.valueOf(number);
	}
	
}
