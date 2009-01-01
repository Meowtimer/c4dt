package net.arctics.clonk.parser.defcore;

import java.security.InvalidParameterException;

import org.eclipse.core.resources.IMarker;


public class UnsignedInteger extends DefCoreOption {
	
	private int number;
	
	public UnsignedInteger(String name) {
		super(name);
	}
	
	public UnsignedInteger(String name, int num) {
		super(name);
		number = num;
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

	@Override
	public void setInput(String input) throws DefCoreParserException {
		try {
			Integer num = Integer.decode(input);
			number = num.intValue();
			if (num < 0)
				throw new DefCoreParserException(IMarker.SEVERITY_WARNING, "Unsigned value expected");
		}
		catch (NumberFormatException e) {
			throw new DefCoreParserException(IMarker.SEVERITY_ERROR, "Invalid value('" + input + "') given for option '" + this.getName() + "'");
		}
	}
	
}
