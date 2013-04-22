package net.arctics.clonk.ini;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.ini.IniData.IniEntryDefinition;
import net.arctics.clonk.util.ArrayUtil;
import net.arctics.clonk.util.IConverter;

import org.eclipse.core.resources.IMarker;

/**
 * A value in an integer array ini entry. Can be either a plain integer value or constant names |-ed together. 
 * @author madeen
 *
 */
public class CategoriesValue extends IniEntryValueBase {
	private List<String> constants = null;
	private int summedValue;
	
	public CategoriesValue() {
		this(0);
	}
	
	public CategoriesValue(int value) {
		summedValue = value;
	}
	
	public CategoriesValue(String value, Engine engine, String constantsPrefix) throws IniParserException {
		this.setInput(value, engine, constantsPrefix);
	}
	
	public void setInput(String input, Engine engine, String constantsPrefix) throws IniParserException {
		constants = null;
		String[] parts = input != null ? input.split("\\|") : new String[0]; //$NON-NLS-1$
		if (parts.length == 1)
			tryIntegerInput(input, parts, engine, constantsPrefix);
		else
			tryConstantInput(input, parts, engine, constantsPrefix);
	}
	
	private void tryIntegerInput(String input, String[] parts, Engine engine, String constantsPrefix) throws IniParserException {
		try {
			int categories = Integer.parseInt(parts[0].trim());
			summedValue = categories;
		} catch(NumberFormatException e) {
			summedValue = -1;
			tryConstantInput(input, parts,engine, constantsPrefix);
		}
	}
	
	private static final IConverter<Variable, String> NAME_MAPPER = new IConverter<Variable, String>() {
		@Override
		public String convert(Variable from) {
			return from.name();
		}
	};
	
	private void tryConstantInput(String input, String[] parts, Engine engine, String constantsPrefix) throws IniParserException {
		constants = new ArrayList<String>(4);
		if (constantsPrefix != null) {
			Variable[] vars = engine.variablesWithPrefix(constantsPrefix);
			String[] varNames = ArrayUtil.map(vars, String.class, NAME_MAPPER);
			for (String part : parts) {
				part = part.trim();
				if (ArrayUtil.indexOf(varNames, part) == -1)
					throw new IniParserException(IMarker.SEVERITY_WARNING, String.format(Messages.UnknownConstant, part));
				constants.add(part);
			}
		}
		else for (String part : parts) {
			part = part.trim();
			Variable var = Core.instance().activeEngine().findVariable(part);
			if (var == null)
				throw new IniParserException(IMarker.SEVERITY_WARNING, String.format(Messages.UnknownConstant, part));
			constants.add(var.name());
		}
	}
	
	public int summedValue() {
		return summedValue;
	}
	
	public void setSummedValue(int summedValue) {
		this.summedValue = summedValue;
	}
	
	public List<String> constants() {
		return constants;
	}
	
	@Override
	public String toString() {
		if (summedValue != -1 || constants == null)
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

	@Override
	public void setInput(String value, IniEntryDefinition entryData, IniUnit context) throws IniParserException {
		setInput(value, context.engine(), entryData.constantsPrefix());
	}
}