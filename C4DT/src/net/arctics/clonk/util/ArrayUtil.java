package net.arctics.clonk.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	public static <T, C> C[] filter(Iterable<T> iterable, Class<C> cls) {
		LinkedList<C> result = new LinkedList<C>();
		for (T item : iterable) {
			if (cls.isAssignableFrom(item.getClass())) {
				result.add((C) item);
			}
		}
		return result.toArray((C[]) Array.newInstance(cls, result.size()));
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
	
	@SuppressWarnings("unchecked")
	public static <From, To> To[] map(From[] elms, Class<To> toClass, IConverter<From, To> converter) {
		To[] result = (To[]) Array.newInstance(toClass, elms.length);
		for (int i = 0; i < result.length; i++)
			result[i] = converter.convert(elms[i]);
		return result;
	}
	
	public static <From, To> Iterable<To> map(Iterable<From> source, IConverter<From, To> converter) {
		return new ConvertingIterable<From, To>(converter, source);
	}
	
	/**
	 * Helper for creating a map with one assignment
	 * @param <KeyType> key type for resulting map
	 * @param <ValueType> value type for resulting map
	 * @param mapClass class the method is to instantiate
	 * @param keysAndValues array containing keys and values. keys are at even indices while values are at uneven ones
	 * @return the map
	 */
	@SuppressWarnings("unchecked")
	public static <KeyType, ValueType> Map<KeyType, ValueType> mapOfType(boolean modifiable, Map<KeyType, ValueType> resultMap, Object... keysAndValues) {
		try {
			for (int i = 0; i < keysAndValues.length-1; i += 2)
				resultMap.put((KeyType)keysAndValues[i], (ValueType)keysAndValues[i+1]);
			return modifiable ? resultMap : Collections.unmodifiableMap(resultMap);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static <KeyType, ValueType> Map<ValueType, KeyType> reverseMap(Map<KeyType, ValueType> originalMap, Map<ValueType, KeyType> resultMap) {
		try {
			for (Map.Entry<KeyType, ValueType> entry : originalMap.entrySet()) {
				resultMap.put(entry.getValue(), entry.getKey());
			}
			return resultMap;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * like mapOfType, but called with HashMap.class
	 * @param <KeyType>
	 * @param <ValueType>
	 * @param keysAndValues
	 * @return
	 */
	public static <KeyType, ValueType> Map<KeyType, ValueType> map(boolean modifiable, Object... keysAndValues) {
		return mapOfType(modifiable, new HashMap<KeyType, ValueType>(), keysAndValues);
	}
	
	public static <A, B> Iterable<B> filteredIterable(final Iterable<A> base, final Class<B> cls) {
		return new FilteredIterable<B, A>(cls, base, false);
	}
	
	// nowhere to be found oO
	/**
	 * Return the index of an item in an array
	 */
	public static <T> int indexOf(T[] items, T item) {
		for (int i = 0; i < items.length; i++)
			if (Utilities.objectsEqual(items[i], item))
				return i;
		return -1;
	}
	
	public static void purgeNullEntries(Collection<?>... collections) {
		for (Collection<?> c : collections)
			if (c != null)
				c.removeAll(Collections.singletonList(null));
	}
	
	public static <T> Set<T> set(T... items) {
		HashSet<T> t = new HashSet<T>();
		for (T i : items)
			t.add(i);
		return t;
	}
	
	public static <T> Set<T> setFromIterable(Iterable<T> iterable) {
		Set<T> set = new HashSet<T>();
		for (T d : iterable)
			set.add(d);
		return set;
	}
	
	public interface λ<Τ> {
		void 跑(Τ on);
	}
	
	public static <T> void forAll(T[] items, λ<T> λ) {
		for (T i : items)
			λ.跑(i);
	}

}
