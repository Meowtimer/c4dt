package net.arctics.clonk.util;

public class APIReflection {
	private static class Typed<T, S extends T> {
		public Typed(S object, Class<T> apiType) {
			super();
			this.object = object;
			this.apiType = apiType;
		}
		public final S object;
		public final Class<T> apiType;
	}
	public static Object call(Object target, String method, Object... arguments) throws ReflectiveOperationException, IllegalArgumentException, SecurityException {
		final Class<?>[] classes = new Class<?>[arguments.length];
		for (int i = 0; i < arguments.length; i++)
			if (arguments[i] instanceof Typed<?,	?>) {
				final Typed<?, ?> apiTyped = (Typed<?, ?>)arguments[i];
				arguments[i] = apiTyped.object;
				classes[i] = apiTyped.apiType;
			} else
				classes[i] = arguments[i].getClass();
		return target.getClass().getMethod(method, classes).invoke(target, arguments);
	}
	public static <T, S extends T> Object typed(S item, Class<T> cls) {
		return new Typed<>(item, cls);
	}
}
