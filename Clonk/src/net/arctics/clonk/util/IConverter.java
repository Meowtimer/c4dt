package net.arctics.clonk.util;

public interface IConverter<From, To> {
	To convert(From from);
}
