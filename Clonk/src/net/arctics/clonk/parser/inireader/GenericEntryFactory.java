package net.arctics.clonk.parser.inireader;

import java.io.InvalidClassException;

import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;

public class GenericEntryFactory implements IEntryFactory {
	
	public Object create(Class<?> type, String value, IniDataEntry entryData) throws InvalidClassException, IniParserException {
		if (value == null)
			value = "";
		if (type.equals(C4ID.class)) {
			return C4ID.getID(value);
		}
		else if (type.equals(String.class)) {
			return value;
		}
		else if (IIniEntry.class.isAssignableFrom(type)) {
			try {
				IIniEntry obj = ((IIniEntry)type.newInstance());
				obj.setInput(value, entryData);
				return obj;
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			throw new InvalidClassException(this.getClass().getName() + " seems not to be capable of constructing objects of type '" + type.getName() + "'");
		}
		else {
			throw new InvalidClassException(this.getClass().getName() + " is not capable of constructing objects of type '" + type.getName() + "'");
		}
	}

}
