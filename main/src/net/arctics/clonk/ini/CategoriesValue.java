package net.arctics.clonk.ini;

import static net.arctics.clonk.util.ArrayUtil.map;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;

import net.arctics.clonk.Core;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.index.Engine;
import net.arctics.clonk.ini.IniData.IniEntryDefinition;
import net.arctics.clonk.util.ArrayUtil;

import org.eclipse.core.resources.IMarker;

/**
 * A value in an integer array ini entry. Can be either a plain integer value or constant names |-ed together.
 * @author madeen
 *
 */
public class CategoriesValue extends IniEntryValue {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private List<String> constants = null;
	private int summedValue;
	public CategoriesValue() { this(0); }
	public CategoriesValue(final int value) { summedValue = value; }
	public CategoriesValue(final String value, final Engine engine, final String constantsPrefix) throws IniParserException {
		this.setInput(value, engine, constantsPrefix);
	}
	public int summedValue() { return summedValue; }
	public void setSummedValue(final int summedValue) { this.summedValue = summedValue; }
	public List<String> constants() { return constants; }

	public void setInput(final String input, final Engine engine, final String constantsPrefix) throws IniParserException {
		constants = null;
		final String[] parts = input != null ? input.split("\\|") : new String[0]; //$NON-NLS-1$
		if (parts.length == 1)
			tryIntegerInput(input, parts, engine, constantsPrefix);
		else
			tryConstantInput(input, parts, engine, constantsPrefix);
	}

	private void tryIntegerInput(final String input, final String[] parts, final Engine engine, final String constantsPrefix) throws IniParserException {
		try {
			final int categories = Integer.parseInt(parts[0].trim());
			summedValue = categories;
		} catch(final NumberFormatException e) {
			summedValue = -1;
			tryConstantInput(input, parts,engine, constantsPrefix);
		}
	}

	private static final Function<Variable, String> NAME_MAPPER = from -> from.name();

	private void tryConstantInput(final String input, final String[] parts, final Engine engine, final String constantsPrefix) throws IniParserException {
		constants = new ArrayList<String>(4);
		if (constantsPrefix != null) {
			final Variable[] vars = engine.variablesWithPrefix(constantsPrefix);
			final String[] varNames = map(vars, String.class, NAME_MAPPER);
			for (String part : parts) {
				part = part.trim();
				if (ArrayUtil.indexOf(varNames, part) == -1)
					throw new IniParserException(IMarker.SEVERITY_WARNING, String.format(Messages.UnknownConstant, part));
				constants.add(part);
			}
		}
		else for (String part : parts) {
			part = part.trim();
			final Variable var = Core.instance().activeEngine().findVariable(part);
			if (var == null)
				throw new IniParserException(IMarker.SEVERITY_WARNING, String.format(Messages.UnknownConstant, part));
			constants.add(var.name());
		}
	}
	@Override
	public String toString() {
		if (summedValue != -1 || constants == null)
			return String.valueOf(summedValue);
		final StringBuilder builder = new StringBuilder(constants.size() * 10); // C4D_Back|
		final ListIterator<String> it = constants.listIterator();
		while (it.hasNext()) {
			builder.append(it.next());
			if (it.hasNext())
				builder.append('|');
		}
		return builder.toString();
	}

	@Override
	public void setInput(final String value, final IniEntryDefinition entryData, final IniUnit context) throws IniParserException {
		setInput(value, context.engine(), entryData.constantsPrefix());
	}
}