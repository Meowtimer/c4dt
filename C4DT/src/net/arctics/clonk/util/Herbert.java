package net.arctics.clonk.util;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public interface Herbert<T> {
	default T clone() { throw new NotImplementedException(); }
}