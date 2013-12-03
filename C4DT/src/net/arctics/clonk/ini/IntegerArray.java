package net.arctics.clonk.ini;


import java.util.Arrays;

import net.arctics.clonk.Core;
import net.arctics.clonk.ini.IniData.IniEntryDefinition;
import net.arctics.clonk.util.IHasChildrenWithContext;
import net.arctics.clonk.util.IHasContext;

import org.eclipse.core.resources.IMarker;

public class IntegerArray extends IniEntryValue implements IHasChildrenWithContext, IConvertibleToPrimitive {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private CategoriesValue[] values;
	public IntegerArray() {}
	public IntegerArray(final int[] values) {
		this.values = new CategoriesValue[values.length];
		for (int i = 0; i < values.length; i++)
			this.values[i] = new CategoriesValue(i);
	}
	public IntegerArray(final String value, final IniEntryDefinition entryData, final IniUnit context) throws IniParserException { setInput(value, entryData, context); }
	public String getStringRepresentation() { return toString(); }
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(values.length * 2);
		for(int i = 0; i < values.length;i++) {
			builder.append(values[i]);
			if (i < values.length - 1) builder.append(","); //$NON-NLS-1$
		}
		return builder.toString();
	}
	@Override
	public void setInput(final String input, final IniEntryDefinition entryData, final IniUnit context) throws IniParserException {
		try {
			// empty input should be okay
			if (input.equals("")) { //$NON-NLS-1$
				this.values = new CategoriesValue[] {};
				return;
			}
			final String[] parts = input.split(","); //$NON-NLS-1$
			if (parts.length > 0) {
				final CategoriesValue[] values = new CategoriesValue[parts.length];
				for(int i = 0; i < parts.length;i++) {
					parts[i] = parts[i].trim();
					if (parts[i].equals("")) //$NON-NLS-1$
						values[i] = new CategoriesValue(0);
					else {
						if (parts[i].startsWith("+")) parts[i] = parts[i].substring(1); //$NON-NLS-1$
						values[i] = new CategoriesValue(parts[i].trim(), context.engine(), entryData.constantsPrefix());
					}
				}
				this.values = values;
			} else
				throw new IniParserException(IMarker.SEVERITY_WARNING, Messages.ExpectedIntegerArray);
		}
		catch(final NumberFormatException e) {
			final IniParserException exp = new IniParserException(IMarker.SEVERITY_ERROR, Messages.ExpectedIntegerArray);
			exp.setInnerException(e);
			throw exp;
		}
	}
	@Override
	public boolean hasChildren() { return values.length > 0; }
	@Override
	public IHasContext[] children(final Object context) {
		final IHasContext[] result = new IHasContext[values.length];
		for (int i = 0; i < result.length; i++)
			result[i] = new EntrySubItem<IntegerArray>(this, context, i);
		return result;
	}
	@Override
	public Object valueOfChildAt(final int index) { return values[index]; }
	public int get(final int index) { return values[index].summedValue(); }
	@Override
	public Object convertToPrimitive() { return values; }
	public CategoriesValue[] values() { return values; }
	public void grow(final int size) { values = Arrays.copyOf(values, Math.max(size, values.length)); }
}
