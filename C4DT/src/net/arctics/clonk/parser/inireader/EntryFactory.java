package net.arctics.clonk.parser.inireader;

import java.io.InvalidClassException;

import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.inireader.IniData.IniEntryDefinition;
import net.arctics.clonk.util.Utilities;

public enum EntryFactory implements IEntryFactory {
	
	INSTANCE;
	
	@Override
	public Object create(Class<?> type, String value, IniEntryDefinition entryData, IniUnit context) throws InvalidClassException, IniParserException {
		if (value == null)
			value = ""; //$NON-NLS-1$
		if (type.equals(ID.class))
			return ID.get(value);
		else if (type.equals(String.class))
			return value;
		else if (IIniEntryValue.class.isAssignableFrom(type)) {
			try {
				IIniEntryValue obj = ((IIniEntryValue)type.newInstance());
				obj.setInput(value, entryData, context);
				return obj;
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			throw new InvalidClassException(this.getClass().getName() + " seems not to be capable of constructing objects of type '" + type.getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (type.isEnum())
			return Utilities.enumValueFromString(type, value);
		else
			throw new InvalidClassException(this.getClass().getName() + " is not capable of constructing objects of type '" + type.getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
