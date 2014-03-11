package net.arctics.clonk.util;

@FunctionalInterface
public interface IConverter<From, To> {
	To convert(From from);
}
