package net.arctics.clonk.ini;

import net.arctics.clonk.index.ID;
import net.arctics.clonk.ini.IniData.IniEntryDefinition;
import net.arctics.clonk.util.Utilities;

public enum EntryFactory implements IEntryFactory {

	INSTANCE;

	@Override
	public Object create(Class<?> type, String value, IniEntryDefinition entryData, IniUnit context) throws IniParserException {
		if (value == null)
			value = ""; //$NON-NLS-1$
		if (type.equals(ID.class))
			return ID.get(value);
		else if (type.equals(String.class))
			return value;
		else if (IIniEntryValue.class.isAssignableFrom(type))
			try {
				IIniEntryValue obj = ((IIniEntryValue)type.newInstance());
				obj.setInput(value, entryData, context);
				return obj;
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
		else if (type.isEnum())
			return Utilities.enumValueFromString(type, value);
		else
			return null;
	}

}
