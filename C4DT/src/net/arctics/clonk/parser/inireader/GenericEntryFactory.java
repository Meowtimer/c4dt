package net.arctics.clonk.parser.inireader;

import java.io.InvalidClassException;

import net.arctics.clonk.parser.ID;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;

public class GenericEntryFactory implements IEntryFactory {
	
	@Override
	public Object create(Class<?> type, String value, IniDataEntry entryData, IniUnit context) throws InvalidClassException, IniParserException {
		if (value == null)
			value = ""; //$NON-NLS-1$
		if (type.equals(ID.class)) {
			return ID.get(value);
		}
		else if (type.equals(String.class)) {
			return value;
		}
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
		}
		else {
			throw new InvalidClassException(this.getClass().getName() + " is not capable of constructing objects of type '" + type.getName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

}
