package net.arctics.clonk.util;

import java.io.IOException;

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
	public static String writeBlock(
			Appendable output,
			CharSequence startBlock, CharSequence endBlock, CharSequence delimiter,
			Iterable<?> enumeration
	) {
		boolean returnString = output == null;
		if (returnString)
			output = new StringBuilder();
		try {
			output.append(startBlock);
			boolean started = false;
			for (Object obj : enumeration) {
				if (started)
					output.append(delimiter);
				else
					started = true;
				output.append(obj.toString());
			}
			output.append(endBlock);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return returnString ? output.toString() : null;
	}
}
