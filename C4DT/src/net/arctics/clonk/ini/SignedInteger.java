package net.arctics.clonk.ini;


import net.arctics.clonk.Core;
import net.arctics.clonk.ini.IniData.IniEntryDefinition;

import org.eclipse.core.resources.IMarker;

public class SignedInteger extends IniEntryValue implements IConvertibleToPrimitive {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	protected long number;
	public SignedInteger(final int i) { number = i; }
	public SignedInteger() {}
	@Override
	public String toString() { return String.valueOf(number); }
	@Override
	public Object convertToPrimitive() { return number; }
	public long number() { return number; }
	public void setNumber(final long number) throws IniParserException { this.number = number; }
	@Override
	public void setInput(String input, final IniEntryDefinition entryData, final IniUnit context) throws IniParserException {
		try {
			input = input != null ? input.trim() : ""; //$NON-NLS-1$
			final int inlineCommentStart = input.indexOf(';');
			if (inlineCommentStart != -1)
				input = input.substring(0, inlineCommentStart).trim();
			if (input.equals("")) //$NON-NLS-1$
				number = 0;
			else
				setNumberFromStringValue(input, entryData, context);
		}
		catch(final NumberFormatException e) {
			throw new IniParserException(IMarker.SEVERITY_ERROR, String.format(Messages.IntegerExpected, input), e);
		}
	}
	protected void setNumberFromStringValue(final String input, final IniEntryDefinition entryData, final IniUnit context) throws IniParserException {
		setNumber(Long.parseLong(input));
	}
}
