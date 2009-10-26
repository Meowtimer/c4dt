package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.parser.C4ID;
import net.arctics.clonk.util.KeyValuePair;

public class IDArray extends KeyValueArrayEntry<C4ID, Integer> {

	@Override
	public KeyValuePair<C4ID, Integer> singleComponentFromString(String s) {
		String[] idAndCount = s.split("="); //$NON-NLS-1$
		if (idAndCount.length < 2)
			return null;
		try {
			return new KeyValuePair<C4ID, Integer>(C4ID.getID(idAndCount[0].trim()), Integer.parseInt(idAndCount[1].trim()));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return null;
		}
	}

}