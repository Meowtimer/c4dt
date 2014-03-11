package net.arctics.clonk.util;

@FunctionalInterface
public interface IPredicate<T> {
	boolean test(T item);
}
