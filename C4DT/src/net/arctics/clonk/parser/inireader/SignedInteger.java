package net.arctics.clonk.parser.inireader;


import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;

import org.eclipse.core.resources.IMarker;

public class SignedInteger extends IniEntryValueBase implements IConvertibleToPrimitive {

	protected long number;
	
	public SignedInteger(int i) {
		number = i;
	}
	
	public SignedInteger() {
	}
	
	public String getStringRepresentation() {
		return Long.toString(number);
	}

	@Override
	public void setInput(String input, IniDataEntry entryData, IniUnit context) throws IniParserException {
		try {
			input = input != null ? input.trim() : ""; //$NON-NLS-1$
			int inlineCommentStart = input.indexOf(';');
			if (inlineCommentStart != -1)
				input = input.substring(0, inlineCommentStart).trim();
			if (input.equals("")) //$NON-NLS-1$
				number = 0;
			else
				setNumber(Long.parseLong(input));
		}
		catch(NumberFormatException e) {
			IniParserException exp = new IniParserException(IMarker.SEVERITY_ERROR, String.format(Messages.IntegerExpected, input)); 
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
	
	public long getNumber() {
		return number;
	}
	
	public void setNumber(long number) throws IniParserException {
		this.number = number;
	}

}
