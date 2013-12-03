package net.arctics.clonk.landscapescript;

import java.io.Serializable;

import net.arctics.clonk.Core;

public final class NumVal implements Serializable {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	
	private final Unit unit;
	private final int value;
	public Unit unit() {
    	return unit;
    }
	public int value() {
    	return value;
    }
	public NumVal(final Unit unit, final int value) {
        super();
        this.unit = unit;
        this.value = value;
    }
	public static NumVal parse(final String value) {
		if (value == null)
			return null;
		int i;
		for (i = value.length()-1; i >= 0 && !Character.isDigit(value.charAt(i)); i--);
		final String unit = value.substring(i+1);
		String number = value.substring(0, i+1);
		if (number.length() > 0 && number.charAt(0) == '+')
			number = number.substring(1); // Integer.parseInt coughs on '+': a lesson in ridiculousness
		return new NumVal(Unit.parse(unit), Integer.parseInt(number));
    }
	@Override
	public String toString() {
	    return value+unit.toString();
	}
}