package net.arctics.clonk.util;

import java.util.Iterator;

public class ConvertingIterable<From, To> implements Iterable<To> {

	private IConverter<From, To> converter;
	private Iterator<From> source;

	public ConvertingIterable(IConverter<From, To> converter, Iterable<From> source) {
		this.converter = converter;
		this.source = source.iterator();
	}
	
	@Override
	public Iterator<To> iterator() {
		return new Iterator<To>() {
			@Override
			public boolean hasNext() {
				return source.hasNext();
			}

			@Override
			public To next() {
				return converter.convert(source.next());
			}

			@Override
			public void remove() {
				source.remove();
			}
		};
	}

}
