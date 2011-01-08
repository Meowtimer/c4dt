package net.arctics.clonk.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ArrayUtil {

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
	
	public static <T, B extends T> T[] arrayRange(B[] source, int start, int length, Class<T> elementClass) {
		@SuppressWarnings("unchecked")
		T[] result = (T[]) Array.newInstance(elementClass, length);
		for (int i = 0; i < length; i++) {
			result[i] = source[start+i];
		}
		return result;
	}
	
	public static <T> T firstOrNull(T[] arr) {
		return arr != null && arr.length > 0 ? arr[0] : null;
	}
	
	public static <T> T[] removeNullElements(T[] array, Class<T> cls) {
		int actualCount = 0;
		for (T t : array)
			if (t != null)
				actualCount++;
		if (actualCount != array.length) {
			@SuppressWarnings("unchecked")
			T[] nonNullIngredients = (T[])Array.newInstance(cls, actualCount);
			actualCount = 0;
			for (T t : array)
				if (t != null)
					nonNullIngredients[actualCount++] = t;
			array = nonNullIngredients;
		}
		return array;
	}
	
	public static <E> ArrayList<E> list(E... elements) {
		ArrayList<E> result = new ArrayList<E>(elements.length);
		for (E e : elements)
			result.add(e);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] convertArray(Object[] baseArray, Class<T> newElementClass) {
		T[] result = (T[]) Array.newInstance(newElementClass, baseArray.length);
		System.arraycopy(baseArray, 0, result, 0, baseArray.length);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] filter(T[] array, IPredicate<T> filter) {
		try {
			List<T> list = Utilities.filter(arrayIterable(array), filter);
			return list.toArray((T[]) Array.newInstance(array.getClass().getComponentType(), list.size()));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <A, B> B[] filter(A[] array, Class<B> cls) {
		List<B> items = new ArrayList<B>(array.length);
		for (A item : array) {
			if (cls.isAssignableFrom(item.getClass())) {
				items.add((B)item);
			}
		}
		return items.toArray((B[]) Array.newInstance(cls, items.size()));
	}

	public static <T> Iterable<T> arrayIterable(final T... items) {
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					private int index = -1;
					public boolean hasNext() {
						for (int i = index+1; i < items.length; i++) {
							if (items[i] != null) {
								return true;
							}
						}
						return false;
					}

					public T next() {
						for (index++; index < items.length; index++) {
							if (items[index] != null) {
								return items[index];
							}
						}
						return null;
					}

					public void remove() {
					}
				};
			}
		};
	}
	
	public static <T> T boundChecked(T[] items, int index) {
		return index >= 0 && index < items.length ? items[index] : null;
	}
	
	public static <T> int indexOf(T item, T[] items) {
		for (int index = 0; index < items.length; index++) {
			if (items[index] == item) {
				return index;
			}
		}
		return -1;
	}

}
