package net.arctics.clonk.util;

public class StringUtil {
	public static String capitalize(String s) {
		if (s == null || s.length() == 0)
			return "";
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
