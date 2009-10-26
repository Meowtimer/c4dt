package net.arctics.clonk.parser.inireader;


import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.util.IHasChildrenWithContext;
import net.arctics.clonk.util.IHasContext;

import org.eclipse.core.resources.IMarker;

public class IntegerArray implements IIniEntry, IHasChildrenWithContext {

	private int[] integers;
	
	public IntegerArray() {
	}
	
	public IntegerArray(String value, IniDataEntry entryData) throws IniParserException {
		setInput(value, entryData);
	}
	
	public String getStringRepresentation() {
		return toString();
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder(integers.length * 2);
		for(int i = 0; i < integers.length;i++) {
			builder.append(integers[i]);
			if (i < integers.length - 1) builder.append(","); //$NON-NLS-1$
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

	public void setInput(String input, IniDataEntry entryData) throws IniParserException {
		try {
			// empty input should be okay
			if (input.equals("")) { //$NON-NLS-1$
				this.integers = new int[] {};
				return;
			}
			String[] parts = input.split(","); //$NON-NLS-1$
			if (parts.length > 0) {
				int[] integers = new int[parts.length];
				for(int i = 0; i < parts.length;i++) {
					parts[i] = parts[i].trim();
					if (parts[i].equals("")) //$NON-NLS-1$
						integers[i] = 0;
					else {
						if (parts[i].startsWith("+")) parts[i] = parts[i].substring(1); //$NON-NLS-1$
						integers[i] = Integer.parseInt(parts[i].trim());
					}
				}
				this.integers = integers;
			}
			else {
				throw new IniParserException(IMarker.SEVERITY_WARNING, Messages.IntegerArray_5);
			}
		}
		catch(NumberFormatException e) {
			IniParserException exp = new IniParserException(IMarker.SEVERITY_ERROR, Messages.IntegerArray_6);
			exp.setInnerException(e);
			throw exp;
		}
	}

	public void set(int index, int value) {
		integers[index] = value;
	}

	public boolean hasChildren() {
		return integers.length > 0;
	}

	public IHasContext[] getChildren(Object context) {
		IHasContext[] result = new IHasContext[integers.length];
		for (int i = 0; i < result.length; i++)
			result[i] = new EntrySubItem<IntegerArray>(this, context, i);
		return result;
	}

	public Object getChildValue(int index) {
		return integers[index];
	}

	public void setChildValue(int index, Object value) {
		integers[index] = value instanceof Integer
			? (Integer)value
			: value instanceof String
				? Integer.valueOf((String)value)
				: 0;
	}	

}
