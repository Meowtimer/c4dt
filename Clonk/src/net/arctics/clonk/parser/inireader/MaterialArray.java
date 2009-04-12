package net.arctics.clonk.parser.inireader;

import net.arctics.clonk.util.KeyValuePair;

public class MaterialArray extends KeyValueArrayEntry<String, String> {

	@Override
	public KeyValuePair<String, String> singleComponentFromString(String s) {
		String[] split = s.split("=");
		if (split.length == 2)
			return new KeyValuePair<String, String>(split[0], split[1]);
		else
			return new KeyValuePair<String, String>(s, "");
	}
	
}
