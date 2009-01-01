package net.arctics.clonk.parser.defcore;

import org.eclipse.core.resources.IMarker;

public class SignedInteger extends DefCoreOption {

	private int x;
	
	public SignedInteger(String name) {
		super(name);
	}
	
	public SignedInteger(String name, int i) {
		super(name);
		x = i;
	}
	
	public String getStringRepresentation() {
		return Integer.toString(x);
	}

	@Override
	public void setInput(String input) throws DefCoreParserException {
		try {
			x = Integer.parseInt(input);
		}
		catch(NumberFormatException e) {
			throw new DefCoreParserException(IMarker.SEVERITY_ERROR, "Expected an integer instead of '" + input + "'");
		}
	}

}
