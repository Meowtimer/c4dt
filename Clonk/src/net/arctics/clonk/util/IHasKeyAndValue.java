package net.arctics.clonk.util;

public interface IHasKeyAndValue<KeyType, ValueType> {
	KeyType getKey();
	ValueType getValue();
}
