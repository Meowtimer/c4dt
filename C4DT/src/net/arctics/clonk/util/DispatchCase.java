package net.arctics.clonk.util;

import static java.util.Arrays.stream;

public class DispatchCase<T, R> {
	public final Class<T> cls;
	public final java.util.function.Function<? super T, R> fn;
	public DispatchCase(Class<T> cls, java.util.function.Function<T, R> fn) {
		super();
		this.cls = cls;
		this.fn = fn;
	}
	public static <T, R> DispatchCase<T, R> caze(Class<T> cls, java.util.function.Function<T, R> fn) {
		return new DispatchCase<T, R>(cls, fn);
	}
	@SafeVarargs
	public static <B, R> R dispatch(B node, DispatchCase<?, R>... fns) {
		@SuppressWarnings("unchecked")
		final DispatchCase<B, R> fn = stream(fns)
			.filter(f -> f.cls.isInstance(node))
			.map(f -> (DispatchCase<B, R>)f)
			.findFirst().orElse(null);
		return fn != null ? fn.fn.apply(node) : null;
	}
}