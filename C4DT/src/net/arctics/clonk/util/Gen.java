package net.arctics.clonk.util;

import java.lang.reflect.Field;

/**
 * Helper class to create instances of classes with just public fields.
 * @author madeen
 *
 */
public class Gen {
	/**
	 * Generate object of given class with instance fields set to the parameters.
	 * @param cls Class of object to create
	 * @param parms Parameters
	 * @return The created object or null if something failed.
	 */
	public static <T> T object(Class<T> cls, Object... parms) {
		try {
			T result = cls.newInstance();
			int parmIndex = 0;
			for (Class<? super T> c = cls; c != null; c = c.getSuperclass()) {
				for (Field f : c.getFields()) {
					for (int i = parmIndex; i < parms.length; i++) {
						Object parm = parms[i];
						if (parm != null && f.getType().isAssignableFrom(parm.getClass())) {
							f.set(result, parm);
							if (parmIndex == i-1)
								parmIndex = i;
							parms[i] = null;
						}
					}
				}
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
