package net.arctics.clonk.parser.defcore;

import org.eclipse.core.resources.IMarker;

public class DefCoreString extends DefCoreOption {

	public String string;
	
	public DefCoreString(String name) {
		super(name);
	}
	
	public DefCoreString(String name, String str) {
		super(name);
		string = str;
	}
	
	public String getStringRepresentation() {
		return string;
	}

	@Override
	public void setInput(String input) throws DefCoreParserException {
		if (input == null) {
			throw new DefCoreParserException(IMarker.SEVERITY_ERROR, "For some reason, value is null which is invalid");
		}
		string = input;
	}

}
