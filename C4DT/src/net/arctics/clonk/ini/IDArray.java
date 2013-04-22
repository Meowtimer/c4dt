package net.arctics.clonk.ini;

import net.arctics.clonk.parser.ID;
import net.arctics.clonk.util.KeyValuePair;

public class IDArray extends KeyValueArrayEntry<ID, Integer> {
	public IDArray() { super(); }
	@SafeVarargs
	public IDArray(KeyValuePair<ID, Integer>... values) {
		for (KeyValuePair<ID, Integer> v : values)
			add(v);
	}
	@Override
	public KeyValuePair<ID, Integer> singleComponentFromString(String s) {
		String[] idAndCount = s.split("="); //$NON-NLS-1$
		if (idAndCount.length < 2)
			return null;
		return new KeyValuePair<ID, Integer>(ID.get(idAndCount[0].trim()), Integer.parseInt(idAndCount[1].trim()));
	}
}