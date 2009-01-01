package net.arctics.clonk.parser.defcore;

import org.eclipse.core.resources.IMarker;

public class IntegerArray extends DefCoreOption {

	private int[] integers;
	
	public IntegerArray(String name) {
		super(name);
	}
	
	public String getStringRepresentation() {
		StringBuilder builder = new StringBuilder(integers.length * 2);
		for(int i = 0; i < integers.length;i++) {
			builder.append(integers[i]);
			if (i < integers.length - 1) builder.append(",");
		}
		return builder.toString();
	}

	public int get(int i) {
		return integers[i];
	}
	
	public int[] getIntegers() {
		return integers;
	}

	public void setIntegers(int[] integers) {
		this.integers = integers;
	}

	@Override
	public void setInput(String input) throws DefCoreParserException {
		try {
			String[] parts = input.split(",");
			if (parts.length > 0) {
				int[] integers = new int[parts.length];
				for(int i = 0; i < parts.length;i++) {
					integers[i] = Integer.parseInt(parts[i]);
				}
			}
			else {
				throw new DefCoreParserException(IMarker.SEVERITY_WARNING, "Expected an integer array");
			}
		}
		catch(NumberFormatException e) {
			throw new DefCoreParserException(IMarker.SEVERITY_ERROR, "Expected an integer array");
		}
	}

}
