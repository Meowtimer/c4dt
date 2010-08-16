package net.arctics.clonk.parser.inireader;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4Engine;
import net.arctics.clonk.parser.c4script.C4Variable;
import net.arctics.clonk.util.IConverter;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.resources.IMarker;

public class CategoriesValue {
	private List<String> constants = null;
	private int summedValue;
	
	public CategoriesValue(int value) {
		summedValue = value;
	}
	
	public CategoriesValue(String value, C4Engine engine, String constantsPrefix) throws IniParserException {
		this.setInput(value, engine, constantsPrefix);
	}
	
	public void setInput(String input, C4Engine engine, String constantsPrefix) throws IniParserException {
		constants = null;
		String[] parts = input != null ? input.split("\\|") : new String[0]; //$NON-NLS-1$
		if (parts.length == 1) {
			tryIntegerInput(input, parts, engine, constantsPrefix);
		}
		else {
			tryConstantInput(input, parts, engine, constantsPrefix);
		}
	}
	
	private void tryIntegerInput(String input, String[] parts, C4Engine engine, String constantsPrefix) throws IniParserException {
		try {
			int categories = Integer.parseInt(parts[0].trim());
			summedValue = categories;
		} catch(NumberFormatException e) {
			summedValue = -1;
			tryConstantInput(input, parts,engine, constantsPrefix);
		}
	}
	
	private void tryConstantInput(String input, String[] parts, C4Engine engine, String constantsPrefix) throws IniParserException {
		constants = new ArrayList<String>(4);
		if (constantsPrefix != null) {
			C4Variable[] vars = engine.variablesWithPrefix(constantsPrefix);
			String[] varNames = Utilities.map(vars, String.class, new IConverter<C4Variable, String>() {
				@Override
				public String convert(C4Variable from) {
					return from.getName();
				}
			});
			for (String part : parts) {
				part = part.trim();
				if (Utilities.indexOf(varNames, part) == -1)
					throw new IniParserException(IMarker.SEVERITY_WARNING, String.format(Messages.UnknownConstant, part));
				constants.add(part);
			}
		}
		else for (String part : parts) {
			part = part.trim();
			C4Variable var = ClonkCore.getDefault().getActiveEngine().findVariable(part);
			if (var == null) {
				throw new IniParserException(IMarker.SEVERITY_WARNING, String.format(Messages.UnknownConstant, part));
			}
			constants.add(var.getName());
		}
	}
	
	public int getSummedValue() {
		return summedValue;
	}
	
	public List<String> getConstants() {
		return constants;
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
}