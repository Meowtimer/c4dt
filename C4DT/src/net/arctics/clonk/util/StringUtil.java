package net.arctics.clonk.util;

public class StringUtil {
	public static String capitalize(String s) {
		if (s == null || s.length() == 0)
			return "";
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
	public static String repetitions(String s, int times) {
		StringBuilder builder = new StringBuilder(s.length()*times);
		for (int i = 0; i < times; i++) {
			builder.append(s);
		}
		return builder.toString();
	}
}
