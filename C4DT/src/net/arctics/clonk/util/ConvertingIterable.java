package net.arctics.clonk.util;

import java.util.Iterator;

public class ConvertingIterable<From, To> implements Iterable<To> {
	private final IConverter<From, To> converter;
	private final Iterable<? extends From> source;
	public ConvertingIterable(final IConverter<From, To> converter, final Iterable<? extends From> source) {
		this.converter = converter;
		this.source = source;
	}
	@Override
	public Iterator<To> iterator() {
		final Iterable<? extends From> _s = source;
		return new Iterator<To>() {
			private final Iterator<? extends From> source = _s.iterator();
			@Override
			public boolean hasNext() { return source.hasNext(); }
			@Override
			public To next() { return converter.convert(source.next()); }
			@Override
			public void remove() { source.remove(); }
		};
	}
}
