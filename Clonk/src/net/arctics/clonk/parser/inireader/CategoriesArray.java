package net.arctics.clonk.parser.inireader;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.resources.IMarker;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.parser.inireader.IniData.IniDataEntry;
import net.arctics.clonk.util.Utilities;

public class CategoriesArray implements IIniEntryValue {

	private final List<String> constants = new ArrayList<String>(4);
	private int summedValue = -1;
	
	public CategoriesArray() {
	}
	
	public void add(String constant) {
		constants.add(constant);
	}
	
	public String toString() {
		if (summedValue != -1)
			return String.valueOf(summedValue);
		StringBuilder builder = new StringBuilder(constants.size() * 10); // C4D_Back|
		ListIterator<String> it = constants.listIterator();
		while (it.hasNext()) {
			builder.append(it.next());
			if (it.hasNext())
				builder.append('|');
		}
		return builder.toString();
	}

	public List<String> getConstants() {
		return constants;
	}

	public void setInput(String input, IniDataEntry entryData) throws IniParserException {
		constants.clear();
		String[] parts = input != null ? input.split("\\|") : new String[0]; //$NON-NLS-1$
		if (parts.length == 1) {
			tryIntegerInput(input, parts, entryData);
		}
		else {
			tryConstantInput(input, parts, entryData);
		}
	}
	
	private void tryIntegerInput(String input, String[] parts, IniDataEntry entryData) throws IniParserException {
		try {
			int categories = Integer.parseInt(parts[0].trim());
			summedValue = categories;
		} catch(NumberFormatException e) {
			summedValue = -1;
			tryConstantInput(input, parts, entryData);
		}
	}
	
	private void tryConstantInput(String input, String[] parts, IniDataEntry entryData) throws IniParserException {
		if (entryData.getFlags() != null) {
			String[] flags = entryData.getFlags();
			for (String part : parts) {
				part = part.trim();
				if (Utilities.indexOf(flags, part) == -1)
					throw new IniParserException(IMarker.SEVERITY_WARNING, Messages.CategoriesArray_1+part+Messages.CategoriesArray_2);
				add(part);
			}
		}
		else for (String part : parts) {
			part = part.trim();
			C4Variable var = ClonkCore.getDefault().getEngineObject().findVariable(part);
			if (var == null) {
				throw new IniParserException(IMarker.SEVERITY_WARNING, Messages.CategoriesArray_3 + part + Messages.CategoriesArray_4);
			}
			add(var.getName());
		}
	}
	
	/**
	 * Only set when the defcore value is the binary sum.
	 * @return the sum or <tt>-1<tt> if categories are defined through concatenation of constant names
	 */
	public int getSummedValue() {
		return summedValue;
	}

}
