package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.ID;
import net.arctics.clonk.util.KeyValuePair;

public class IDArray extends KeyValueArrayEntry<ID, Integer> {

	@Override
	public KeyValuePair<ID, Integer> singleComponentFromString(String s) {
		String[] idAndCount = s.split("="); //$NON-NLS-1$
		if (idAndCount.length < 2)
			return null;
		try {
			return new KeyValuePair<ID, Integer>(ID.get(idAndCount[0].trim()), Integer.parseInt(idAndCount[1].trim()));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return null;
		}
	}

}