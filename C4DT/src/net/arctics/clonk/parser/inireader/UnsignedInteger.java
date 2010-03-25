package net.arctics.clonk.parser.inireader;

import java.security.InvalidParameterException;


import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;

import org.eclipse.core.resources.IMarker;


public class UnsignedInteger implements IIniEntryValue, IConvertibleToPrimitive {
	
	private int number;
	
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
		else throw new InvalidParameterException(Messages.OnlyUnsignedIntegersAllowed);
	}

	public String getStringRepresentation() {
		return Integer.toString(this.number);
	}

	@Override
	public void setInput(String input, IniDataEntry entryData, IniUnit context) throws IniParserException {
		try {
			input = input.trim();
			Integer num = !input.equals("") ? Integer.decode(input) : 0; //$NON-NLS-1$
			number = num.intValue();
			if (num < 0)
				throw new IniParserException(IMarker.SEVERITY_WARNING, Messages.UnsignedValueExpected);
		}
		catch (NumberFormatException e) {
			IniParserException exp = new IniParserException(IMarker.SEVERITY_ERROR, String.format(Messages.InvalidValue, input));
			exp.setInnerException(e);
			throw exp;
		}
	}
	
	@Override
	public String toString() {
		return String.valueOf(number);
	}

	@Override
	public Object convertToPrimitive() {
		return number;
	}
	
}
