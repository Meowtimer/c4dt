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

import net.arctics.clonk.util.Sink.Decision;

public class ArrayUtil {

	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static <T> T[] concat(final T[] a, final T... b) {
		final int alen = a != null ? a.length : 0;
		final int blen = b != null ? b.length : 0;
		if (alen == 0)
			return b != null ? b : (T[])new Object[0];
		if (blen == 0)
			return a != null ? a : (T[])new Object[0];
		final T[] result = (T[]) Array.newInstance(Utilities.baseClass(
			a.getClass().getComponentType(),
			b.getClass().getComponentType()
		), alen+blen);
		System.arraycopy(a, 0, result, 0, alen);
		System.arraycopy(b, 0, result, alen, blen);
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] concat(final T first, final T... rest) {
		final T[] result = (T[]) Array.newInstance(rest.getClass().getComponentType(), 1+rest.length);
		result[0] = first;
		for (int i = 0; i < rest.length; i++)
			result[1+i] = rest[i];
		return result;
	}

	public static <T, B extends T> T[] arrayRange(final B[] source, final int start, final int length, final Class<T> elementClass) {
		@SuppressWarnings("unchecked")
		final
		T[] result = (T[]) Array.newInstance(elementClass, length);
		for (int i = 0; i < length; i++)
			result[i] = source[start+i];
		return result;
	}

	public static <T> T firstOrNull(final T[] arr) {
		return arr != null && arr.length > 0 ? arr[0] : null;
	}

	public static <T> T[] removeNullElements(T[] array, final Class<T> cls) {
		int actualCount = 0;
		for (final T t : array)
			if (t != null)
				actualCount++;
		if (actualCount != array.length) {
			@SuppressWarnings("unchecked")
			final
			T[] nonNullIngredients = (T[])Array.newInstance(cls, actualCount);
			actualCount = 0;
			for (final T t : array)
				if (t != null)
					nonNullIngredients[actualCount++] = t;
			array = nonNullIngredients;
		}
		return array;
	}

	@SuppressWarnings("unchecked")
	public static <E> ArrayList<E> list(final E... elements) {
		final ArrayList<E> result = new ArrayList<E>(elements.length);
		for (final E e : elements)
			result.add(e);
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] convertArray(final Object[] baseArray, final Class<T> newElementClass) {
		final T[] result = (T[]) Array.newInstance(newElementClass, baseArray.length);
		System.arraycopy(baseArray, 0, result, 0, baseArray.length);
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] filter(final T[] array, final IPredicate<T> filter) {
		try {
			final List<T> list = filter(iterable(array), filter);
			return list.toArray((T[]) Array.newInstance(array.getClass().getComponentType(), list.size()));
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static <T> List<T> filter(final Iterable<? extends T> iterable, final IPredicate<T> filter) {
		final List<T> result = new LinkedList<T>();
		for (final T elm : iterable)
			if (filter.test(elm))
				result.add(elm);
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <T, C> C[] filter(final Iterable<T> iterable, final Class<C> cls) {
		final LinkedList<C> result = new LinkedList<C>();
		for (final T item : iterable)
			if (cls.isAssignableFrom(item.getClass()))
				result.add((C) item);
		return result.toArray((C[]) Array.newInstance(cls, result.size()));
	}

	@SuppressWarnings("unchecked")
	public static <A, B> B[] filter(final A[] array, final Class<B> cls) {
		final List<B> items = new ArrayList<B>(array.length);
		for (final A item : array)
			if (cls.isAssignableFrom(item.getClass()))
				items.add((B)item);
		return items.toArray((B[]) Array.newInstance(cls, items.size()));
	}

	@SuppressWarnings("unchecked")
	public static <T> Iterable<T> iterable(final T... items) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					private int index = -1;
					@Override
					public boolean hasNext() {
						for (int i = index+1; i < items.length; i++)
							if (items[i] != null)
								return true;
						return false;
					}
					@Override
					public T next() {
						for (index++; index < items.length; index++)
							if (items[index] != null)
								return items[index];
						return null;
					}
					@Override
					public void remove() {}
				};
			}
		};
	}

	public static <T> T boundChecked(final T[] items, final int index) {
		return index >= 0 && index < items.length ? items[index] : null;
	}

	public static <T> int indexOf(final T item, final T[] items) {
		for (int index = 0; index < items.length; index++)
			if (items[index] == item)
				return index;
		return -1;
	}

	@SuppressWarnings("unchecked")
	public static <From, To> To[] map(final From[] elms, final Class<To> toClass, final IConverter<From, To> converter) {
		final To[] result = (To[]) Array.newInstance(toClass, elms.length);
		for (int i = 0; i < result.length; i++)
			result[i] = converter.convert(elms[i]);
		return result;
	}

	public static <T> Map<T, Integer> mapValueToIndex(final T[] items) {
		final Map<T, Integer> result = new HashMap<T, Integer>();
		for (int i = 0; i < items.length; i++)
			result.put(items[i], i);
		return result;
	}

	public static <From, To> Iterable<To> map(final Iterable<? extends From> source, final IConverter<From, To> converter) {
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
	public static <KeyType, ValueType> Map<KeyType, ValueType> mapOfType(final boolean modifiable, final Map<KeyType, ValueType> resultMap, final Object... keysAndValues) {
		try {
			for (int i = 0; i < keysAndValues.length-1; i += 2)
				resultMap.put((KeyType)keysAndValues[i], (ValueType)keysAndValues[i+1]);
			return modifiable ? resultMap : Collections.unmodifiableMap(resultMap);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static <KeyType, ValueType> Map<ValueType, KeyType> reverseMap(final Map<KeyType, ValueType> originalMap, final Map<ValueType, KeyType> resultMap) {
		try {
			for (final Map.Entry<KeyType, ValueType> entry : originalMap.entrySet())
				resultMap.put(entry.getValue(), entry.getKey());
			return resultMap;
		} catch (final Exception e) {
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
	public static <KeyType, ValueType> Map<KeyType, ValueType> map(final boolean modifiable, final Object... keysAndValues) {
		return mapOfType(modifiable, new HashMap<KeyType, ValueType>(), keysAndValues);
	}

	public static <A, B> Iterable<B> filteredIterable(final Iterable<A> base, final Class<B> cls) {
		return new FilteredIterable<B, A>(cls, base, false);
	}

	// nowhere to be found oO
	/**
	 * Return the index of an item in an array
	 */
	public static <T> int indexOf(final T[] items, final T item) {
		for (int i = 0; i < items.length; i++)
			if (Utilities.eq(items[i], item))
				return i;
		return -1;
	}

	public static void purgeNullEntries(final Collection<?>... collections) {
		for (final Collection<?> c : collections)
			if (c != null)
				c.removeAll(Collections.singletonList(null));
	}

	public static <T> Set<T> set(@SuppressWarnings("unchecked") final T... items) {
		final HashSet<T> t = new HashSet<T>();
		for (final T i : items)
			t.add(i);
		return t;
	}

	public static <T> Set<? extends T> setFromIterable(final Iterable<T> iterable) {
		final Set<T> set = new HashSet<T>();
		for (final T d : iterable)
			set.add(d);
		return set;
	}

	public static <T extends Comparable<? super T>> List<T> asSortedList(final Collection<T> c) {
		final List<T> list = new ArrayList<T>(c);
		Collections.sort(list);
		return list;
	}

	public static <T> List<T> listFromIterable(final Iterable<T> iterable) {
		final LinkedList<T> result = new LinkedList<T>();
		for (final T t : iterable)
			result.add(t);
		return result;
	}

	public static <T> List<? extends T> copyListOrReturnDefaultList(final Collection<? extends T> list, final List<? extends T> defaultResult) {
		if (list == null)
			return defaultResult;
		else synchronized (list) {
			return new ArrayList<T>(list);
		}
	}

	public static <T> void addAllSynchronized(final Collection<? extends T> list, final List<T> into, Object lock) {
		if (lock == null)
			lock = list;
		if (list != null && lock != null)
			synchronized (lock) {
				into.addAll(list);
			}
	}

	public static <T> List<T> list(final Iterable<? extends T> iterable) {
		final List<T> result = new ArrayList<T>();
		for (final T i : iterable)
			result.add(i);
		return result;
	}

	public static <T> Decision sink(final Iterable<? extends T> iterable, final Sink<T> sink) {
		final Iterator<? extends T> it = iterable.iterator();
		while (it.hasNext())
			switch (sink.elutriate(it.next())) {
			case PurgeItem:
				it.remove();
				break;
			case AbortIteration:
				return Decision.AbortIteration;
			default:
				break;
			}
		return Decision.Continue;
	}

	public static <T> boolean elementsEqual(final T[] a, final T[] b) {
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; i++)
			if (!Utilities.eq(a[i], b[i]))
				return false;
		return true;
	}

	public static <T> int pack(final T[] array) {
		int i, j;
		for (i = 0, j = 0; i < array.length; i++)
			if (array[i] != null)
				array[j++] = array[i];
		return j;
	}

	public interface Folder<T, Y extends T> {
		Y fold(T interim, T next, int index);
	}

	public static <T, Y extends T> Y foldl(final Iterable<? extends T> iterable, final Folder<T, Y> folder) {
		Y interim = null;
		T first = null;
		int i = 0;
		for (final T item : iterable) {
			if (interim == null) {
				if (first != null)
					interim = folder.fold(first, item, i);
				else
					first = item;
			} else
				interim = folder.fold(interim, item, i);
			i++;
		}
		return interim;
	}

	public static <T> Sink<? super T> collectionSink(final Collection<? super T> collection) {
		return new Sink<T>() {
			@Override
			public void receivedObject(final T item) {
				collection.add(item);
			}
		};
	}

}
