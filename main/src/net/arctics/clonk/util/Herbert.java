package net.arctics.clonk.util;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/** http://bugs.java.com/view_bug.do?bug_id=4098033 */
public interface Herbert<T> {
	default T clone() { throw new NotImplementedException(); }
}