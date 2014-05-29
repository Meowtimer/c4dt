package net.arctics.clonk.util;

public interface ICreate<T> {
	T create();
	Class<T> cls();
}
