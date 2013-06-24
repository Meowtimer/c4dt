package net.arctics.clonk.ini;

public abstract class IniEntryValueBase implements IIniEntryValue {
	@Override
	public Object evaluate(Object context) { return toString(); }
	public boolean isEmpty() { return false; }
}