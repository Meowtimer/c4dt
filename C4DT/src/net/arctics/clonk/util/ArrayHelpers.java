package net.arctics.clonk.util;

import java.lang.reflect.Array;

public class ArrayHelpers {

	@SuppressWarnings("unchecked")
	public static <T> T[] concat(T[] a, T... b) {
		final int alen = a != null ? a.length : 0;
		final int blen = b != null ? b.length : 0;
		if (alen == 0) {
			return b != null ? b : (T[])new Object[0];
		}
		if (blen == 0) {
			return a != null ? a : (T[])new Object[0];
		}
		final T[] result = (T[]) Array.newInstance(Utilities.baseClass(a.getClass().getComponentType(), b.getClass().getComponentType()), alen+blen);
		System.arraycopy(a, 0, result, 0, alen);
		System.arraycopy(b, 0, result, alen, blen);
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] concat(T first, T... rest) {
		T[] result = (T[]) Array.newInstance(rest.getClass().getComponentType(), 1+rest.length);
		result[0] = first;
		for (int i = 0; i < rest.length; i++)
			result[1+i] = rest[i];
		return result;
	}

}
