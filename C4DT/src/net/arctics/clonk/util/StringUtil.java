package net.arctics.clonk.util;

import java.io.IOException;

/**
 * String utilty functions.
 * @author madeen
 *
 */
public class StringUtil {
	/**
	 * Return a copy of the first string with the first character capitalized
	 * @param s The string to return a capitalized version of
	 * @return The capitalized version
	 */
	public static String capitalize(String s) {
		if (s == null || s.length() == 0)
			return "";
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
	/**
	 * Return a string consisting of times repetitions of s
	 * @param s The string to repeat
	 * @param times The number of repititions
	 * @return The string containing the repititions
	 */
	public static String repetitions(String s, int times) {
		StringBuilder builder = new StringBuilder(s.length()*times);
		for (int i = 0; i < times; i++) {
			builder.append(s);
		}
		return builder.toString();
	}
	/**
	 * Write a block of items, consisting of a start string, an end string, a delimiter string and the elements which are obtained from some {@link Iterable}
	 * @param output The {@link Appendable} the resulting string will be appended to
	 * @param startBlock The start block string
	 * @param endBlock The end block string
	 * @param delimiter The delimiter string
	 * @param enumeration The elements {@link Iterable}
	 * @return If output is set to null, the resulting string will be returned. Null is returned if there is an output to append the result to.
	 */
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
	/**
	 * Evaluate escapes such as \" and \\
	 * @param str The string containing escapes
	 * @return The str with escapes evaluated
	 */
	public static String evaluateEscapes(String str) {
		StringBuilder sBuilder = new StringBuilder(str.length());
		int len = str.length();
		for (int i = 0; i < len; i++) {
			if (i < len-1) switch (str.charAt(i)) {
			case '\\':
				switch (str.charAt(i+1)) {
				case '\\': case '"':
					sBuilder.append(str.charAt(++i));
					continue;
				}
				break;
			}
			sBuilder.append(str.charAt(i));
		}
		return sBuilder.toString();
	}
	
	public static String wildcardToRegex(String wildcard){
        StringBuffer s = new StringBuffer(wildcard.length());
        s.append('^');
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch(c) {
                case '*':
                    s.append(".*");
                    break;
                case '?':
                    s.append(".");
                    break;
                    // escape special regexp-characters
                case '(': case ')': case '[': case ']': case '$':
                case '^': case '.': case '{': case '}': case '|':
                case '\\':
                    s.append("\\");
                    s.append(c);
                    break;
                default:
                    s.append(c);
                    break;
            }
        }
        s.append('$');
        return(s.toString());
    }
}
