package net.arctics.clonk.parser.inireader;

import org.eclipse.core.resources.IMarker;

public class UnsignedInteger extends SignedInteger {

	public UnsignedInteger(int num) {
		super(num);
	}
	
	public UnsignedInteger() {
	}

	@Override
	public void setNumber(long number) throws IniParserException {
		if (number < 0)
			throw new IniParserException(IMarker.SEVERITY_ERROR, Messages.OnlyUnsignedIntegersAllowed);
		super.setNumber(number);
	}
	
}
