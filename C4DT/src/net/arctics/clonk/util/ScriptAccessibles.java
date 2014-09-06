package net.arctics.clonk.util;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static net.arctics.clonk.util.StringUtil.blockString;
import static net.arctics.clonk.util.Utilities.memoize;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ScriptAccessibles {

	private ScriptAccessibles() {}

	@FunctionalInterface
	public interface Callable {
		Object invoke(Object target, Object... parameters) throws Exception;
	}

	private static class OverloadResolvingCallable implements Callable {
		private final List<Method> methods;
		public OverloadResolvingCallable(List<Method> methods) {
			this.methods = methods;
		}
		@Override
		public Object invoke(Object target, Object... parameters) throws Exception {
			for (final Method m : methods)
				try {
					return m.invoke(target, parameters);
				} catch (final ReflectiveOperationException r) {
					continue;
				} catch (final Exception e) {
					e.printStackTrace();
					return null;
				}
			throw new IllegalArgumentException(format(
				"No overload of '%s' found matching '%s'",
				this.methods.get(0).getName(),
				blockString("", "", ", ", stream(parameters))
			));
		}
	}

	private static boolean hasAnnotation(Method m) {
		if (m.getAnnotation(ScriptAccessible.class) != null)
			return true;
		final Class<?> supr = m.getDeclaringClass().getSuperclass();
		try {
			final Method suprMeth = supr != null ? supr.getMethod(m.getName(), m.getParameterTypes()) : null;
			return suprMeth != null && suprMeth != m && hasAnnotation(suprMeth);
		} catch (final ReflectiveOperationException r) {
			return false;
		}
	}

	static Map<String, Callable> getMethods(Class<?> cls, Predicate<Method> filter) {
		return stream(cls.getMethods())
			.filter(ScriptAccessibles::hasAnnotation)
			.filter(filter)
			.collect(Collectors.groupingBy(m -> m.getName()))
			.entrySet().stream()
			.collect(Collectors.toMap(
				e -> e.getKey(),
				e -> e.getValue().size() == 1
					? (Callable)e.getValue().get(0)::invoke
					: new OverloadResolvingCallable(e.getValue())
			));
	}

	static Map<String, Callable> getAllMethods(Class<?> cls) {
		return getMethods(cls, m -> true);
	}

	static Map<String, Callable> getGetters(Class<?> cls) {
		return getMethods(cls, m -> m.getParameterCount() == 0 && m.getReturnType() != Void.TYPE);
	}

	static Map<String, Callable> getSetters(Class<?> cls) {
		return getMethods(cls, m -> m.getParameterCount() == 1 && m.getReturnType() == Void.TYPE);
	}

	static Constructor<?> getCtor(Class<?> cls) {
		return stream(cls.getConstructors())
			.filter(m -> m.getAnnotation(ScriptAccessible.class) != null)
			.findFirst().orElse(null);
	}

	@FunctionalInterface
	interface ThrowHappyFunction<I, O> {
		O apply(I input) throws Exception;
	}

	private static Function<Class<?>, Map<String, Callable>> cadge(ThrowHappyFunction<Class<?>, Map<String, Callable>> f) {
		return input -> {
			try {
				return f.apply(input);
			} catch (final Exception x) {
				x.printStackTrace();
				return new HashMap<>();
			}
		};
	}

	private static final Function<Class<?>, Map<String, Callable>> methods = memoize(cadge(ScriptAccessibles::getAllMethods));
	private static final Function<Class<?>, Map<String, Callable>> getters = memoize(cadge(ScriptAccessibles::getGetters));
	private static final Function<Class<?>, Map<String, Callable>> setters = memoize(cadge(ScriptAccessibles::getSetters));
	private static final Function<Class<?>, Constructor<?>> ctor = memoize(ScriptAccessibles::getCtor);

	public static Constructor<?> ctor(Class<?> cls) { return ctor.apply(cls); }
	public static Callable method(Class<?> cls, String name) { return methods.apply(cls).get(name); }
	public static Callable getter(Class<?> cls, String name) { return getters.apply(cls).get(name); }
	public static Callable setter(Class<?> cls, String name) { return setters.apply(cls).get(name); }
}
