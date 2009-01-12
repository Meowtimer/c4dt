package net.arctics.clonk.parser.inireader;

import java.io.InvalidClassException;

import net.arctics.clonk.parser.C4ID;

public class DefCoreFactory implements IEntryFactory {
	
	public Object create(Class<?> type, String value) throws InvalidClassException, IniParserException {
		if (type.equals(C4ID.class)) {
			return C4ID.getID(value);
		}
		else if (type.equals(String.class)) {
			return value;
		}
		else if (IEntryCreateable.class.isAssignableFrom(type)) {
			try {
				IEntryCreateable obj = ((IEntryCreateable)type.newInstance());
				obj.setInput(value);
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
