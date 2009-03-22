package net.arctics.clonk.parser.inireader;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.resources.IMarker;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.C4Variable;

public class CategoriesArray implements IEntryCreateable {

	private final List<C4Variable> constants = new ArrayList<C4Variable>(4);
	private int summedValue = -1;
	
	public CategoriesArray() {
	}
	
	public CategoriesArray(String input) throws IniParserException {
		setInput(input);
	}
	
	public void add(C4Variable var) {
		constants.add(var);
	}
	
	public String toString() {
		if (summedValue != -1)
			return String.valueOf(summedValue);
		StringBuilder builder = new StringBuilder(constants.size() * 10); // C4D_Back|
		ListIterator<C4Variable> it = constants.listIterator();
		while (it.hasNext()) {
			C4Variable var = it.next();
			builder.append(var.getName());
			if (it.hasNext()) builder.append('|');
		}
		return builder.toString();
	}

	public List<C4Variable> getConstants() {
		return constants;
	}

	public void setInput(String input) throws IniParserException {
		String[] parts = input.split("\\|");
		if (parts.length == 1) {
			tryIntegerInput(input, parts);
		}
		else {
			tryConstantInput(input, parts);
		}
	}
	
	private void tryIntegerInput(String input, String[] parts) throws IniParserException {
		try {
			int categories = Integer.parseInt(parts[0].trim());
			summedValue = categories;
		} catch(NumberFormatException e) {
			summedValue = -1;
			tryConstantInput(input, parts);
		}
	}
	
	private void tryConstantInput(String input, String[] parts) throws IniParserException {
		for(int i = 0; i < parts.length;i++) {
			C4Variable var = ClonkCore.getDefault().ENGINE_OBJECT.findVariable(parts[i].trim());
			if (var == null) {
				throw new IniParserException(IMarker.SEVERITY_WARNING, "Unknown constant '" + parts[i].trim() + "'");
			}
			add(var);
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
