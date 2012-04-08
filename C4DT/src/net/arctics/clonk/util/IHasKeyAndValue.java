package net.arctics.clonk.util;

public interface IHasKeyAndValue<KeyType, ValueType> {
	KeyType key();
	ValueType stringValue();
	void setStringValue(ValueType value, Object context);
}
